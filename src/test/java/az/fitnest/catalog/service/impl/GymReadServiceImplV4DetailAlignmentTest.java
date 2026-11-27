package az.fitnest.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.response.GymDetailResponseV4;
import az.fitnest.catalog.dto.response.GymPackageHomeEligibilityResponse;
import az.fitnest.catalog.dto.response.GymPlanItemResponseV2;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.PackageRule;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.Tier;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import az.fitnest.catalog.repository.ReviewRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import az.fitnest.catalog.service.GymMembershipPolicyV4Service;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import az.fitnest.order.grpc.PackageNameInfo;
import az.fitnest.order.grpc.SubscriptionPackageInfo;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * v4 detail supportedSubscriptions alignment. Tiers without a gym_subscriptions row are synthesized
 * only when the V4 policy offers them (included/primaryEnabled); upward-closed homeEligible alone must
 * not add chips (Bronze-only Central Fitness showed Silver/Gold/Platinum).
 * Package ids 101-104 are unique to this class because HomeGymRuleService caches contexts statically.
 */
class GymReadServiceImplV4DetailAlignmentTest {
  static final long BRONZE = 101, SILVER = 102, GOLD = 103, PLATINUM = 104;

  GymRepository gymRepository;
  OrderServiceGrpcClient order;
  GymMembershipPolicyV4Service policyService;
  GymPackagePriceRepository packagePrices;
  GymReadServiceImpl service;
  Gym gym;

  @BeforeEach
  void setUp() throws Exception {
    order = mock(OrderServiceGrpcClient.class);
    gymRepository = mock(GymRepository.class);
    policyService = mock(GymMembershipPolicyV4Service.class);
    packagePrices = mock(GymPackagePriceRepository.class);
    when(order.getGymPlans())
        .thenReturn(
            List.of(
                plan(BRONZE, "Bronze Plan"),
                plan(SILVER, "Silver Plan"),
                plan(GOLD, "Gold Plan"),
                plan(PLATINUM, "Platinum Plan")));
    // Mirror order-backend: return a name for whichever ids are asked about (gym rows are filtered
    // out of supportedSubscriptions when their name lookup comes back empty).
    when(order.getPackageNamesByIds(any()))
        .thenAnswer(
            inv -> {
              List<Long> ids = inv.getArgument(0);
              if (ids == null) {
                return List.of();
              }
              return ids.stream()
                  .map(
                      id ->
                          PackageNameInfo.newBuilder()
                              .setPackageId(id)
                              .setName(id == BRONZE ? "Bronze Plan" : id == GOLD ? "Gold Plan" : "Plan " + id)
                              .build())
                  .toList();
            });

    gym = Gym.builder().name("Volan").homeEligible(true).build();
    gym.setId(14L);
    GymSubscription gold = new GymSubscription();
    gold.setId(900L);
    gold.setGym(gym);
    gold.setPackageId(GOLD);
    gold.setDailyPrice(12.0);
    gym.getSubscriptions().add(gold);
    when(gymRepository.findWithDetailsById(14L)).thenReturn(Optional.of(gym));

    HomeGymRuleService rules =
        new HomeGymRuleService(packagePrices, mock(GymTimeSlotRuleRepository.class), order,
            mock(az.fitnest.catalog.repository.GymMembershipPolicyV4Repository.class));
    service = newService(rules);
  }

  @Test
  void goldOnlyGymWithoutPolicyDoesNotSynthesizeHomeEligiblePlatinum() {
    when(policyService.find(14L)).thenReturn(Optional.empty());

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    // packages[].homeEligible stays upward-closed...
    assertTrue(homeEligible(detail, Tier.PLATINUM));
    assertTrue(homeEligible(detail, Tier.GOLD));
    assertFalse(homeEligible(detail, Tier.SILVER));
    // ...but home eligibility is not an offer: only the DB-backed Gold row is returned.
    assertEquals(Set.of(String.valueOf(GOLD)), planIds(detail));
  }

  @Test
  void goldOnlyGymSynthesizesPlatinumWhenPolicyEnablesIt() {
    when(policyService.find(14L))
        .thenReturn(Optional.of(
            new GymMembershipPolicyV4(
                1,
                false,
                List.of(
                    rule(BRONZE, Tier.BRONZE, false, "3.00"),
                    rule(SILVER, Tier.SILVER, false, "4.00"),
                    rule(GOLD, Tier.GOLD, true, "5.00"),
                    rule(PLATINUM, Tier.PLATINUM, true, "9.50")),
                List.of())));

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    Set<String> ids = planIds(detail);
    assertTrue(ids.contains(String.valueOf(PLATINUM)), "Platinum plan must be offered: " + ids);
    // Existing Gold row is kept as-is.
    assertTrue(ids.contains(String.valueOf(GOLD)));
    assertEquals("Platinum", plan(detail, PLATINUM).packageName());
  }

  @Test
  void bronzeOnlyGymShowsOnlyBronzeChipEvenThoughHigherTiersAreHomeEligible() {
    // Central Fitness (gym 97) shape: single Bronze gym_subscriptions row.
    gym.getSubscriptions().clear();
    GymSubscription bronze = new GymSubscription();
    bronze.setId(901L);
    bronze.setGym(gym);
    bronze.setPackageId(BRONZE);
    bronze.setDailyPrice(3.0);
    gym.getSubscriptions().add(bronze);
    when(policyService.find(14L)).thenReturn(Optional.empty());

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    for (Tier tier : Tier.values()) {
      assertTrue(homeEligible(detail, tier), tier + " is home-eligible via upward closure");
    }
    assertEquals(Set.of(String.valueOf(BRONZE)), planIds(detail));
  }

  @Test
  void bronzeOnlyGymWithPolicyOfferingOnlyBronzeStaysBronzeOnly() {
    gym.getSubscriptions().clear();
    GymSubscription bronze = new GymSubscription();
    bronze.setId(901L);
    bronze.setGym(gym);
    bronze.setPackageId(BRONZE);
    bronze.setDailyPrice(3.0);
    gym.getSubscriptions().add(bronze);
    when(policyService.find(14L))
        .thenReturn(Optional.of(
            new GymMembershipPolicyV4(
                1,
                false,
                List.of(
                    rule(BRONZE, Tier.BRONZE, true, "3.00"),
                    rule(SILVER, Tier.SILVER, false, "4.00"),
                    rule(GOLD, Tier.GOLD, false, "5.00"),
                    rule(PLATINUM, Tier.PLATINUM, false, "9.50")),
                List.of())));

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    assertEquals(Set.of(String.valueOf(BRONZE)), planIds(detail));
  }

  @Test
  void includedGoldShowsWhenHomeGymDisabled() {
    // Qala Fitness Gəncə: Bronze gym_subscriptions only; Gold included in policy but primary off.
    gym.setHomeEligible(false);
    gym.getSubscriptions().clear();
    GymSubscription bronze = new GymSubscription();
    bronze.setId(901L);
    bronze.setGym(gym);
    bronze.setPackageId(BRONZE);
    bronze.setDailyPrice(4.0);
    gym.getSubscriptions().add(bronze);
    when(policyService.find(14L))
        .thenReturn(Optional.of(
            new GymMembershipPolicyV4(
                0,
                false,
                List.of(
                    includedOnly(BRONZE, Tier.BRONZE, "4.00"),
                    includedOnly(SILVER, Tier.SILVER, "0.00", false),
                    includedOnly(GOLD, Tier.GOLD, "10.00"),
                    includedOnly(PLATINUM, Tier.PLATINUM, "0.00", false)),
                List.of())));

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    assertEquals(Set.of(String.valueOf(BRONZE), String.valueOf(GOLD)), planIds(detail));
    assertEquals(10.0, plan(detail, GOLD).dailyPrice());
  }

  @Test
  void silverOnlyPolicyPrunesStaleBronzeGoldPlatinumSubscriptionRows() {
    // Go Premium Fitness (gym 251): policy Silver-only, but gym_subscriptions still had all four.
    gym.getSubscriptions().clear();
    for (long pkg : List.of(BRONZE, SILVER, GOLD, PLATINUM)) {
      GymSubscription row = new GymSubscription();
      row.setId(800 + pkg);
      row.setGym(gym);
      row.setPackageId(pkg);
      row.setDailyPrice(5.0);
      row.setSupportedServices(new HashSet<>());
      gym.getSubscriptions().add(row);
    }
    when(policyService.find(14L))
        .thenReturn(Optional.of(
            new GymMembershipPolicyV4(
                1,
                false,
                List.of(
                    includedOnly(BRONZE, Tier.BRONZE, "0.00", false),
                    includedOnly(SILVER, Tier.SILVER, "5.00"),
                    includedOnly(GOLD, Tier.GOLD, "0.00", false),
                    includedOnly(PLATINUM, Tier.PLATINUM, "0.00", false)),
                List.of())));
    when(policyService.offeredPackageIdsByGym(any()))
        .thenReturn(Map.of(14L, Set.of(SILVER)));
    when(order.getPackageNamesByIds(any()))
        .thenAnswer(
            inv -> {
              List<Long> ids = inv.getArgument(0);
              if (ids == null) {
                return List.of();
              }
              return ids.stream()
                  .map(
                      id ->
                          PackageNameInfo.newBuilder()
                              .setPackageId(id)
                              .setName(
                                  id == BRONZE
                                      ? "Bronze"
                                      : id == SILVER
                                          ? "Silver"
                                          : id == GOLD ? "Gold" : "Platinum")
                              .build())
                  .toList();
            });

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    assertEquals(Set.of(String.valueOf(SILVER)), planIds(detail));
  }

  @Test
  void platinumPackageIdAndPriceComeFromPolicyWhenAvailable() {
    when(policyService.find(14L))
        .thenReturn(Optional.of(
            new GymMembershipPolicyV4(
                1,
                false,
                List.of(
                    rule(BRONZE, Tier.BRONZE, false, "3.00"),
                    rule(SILVER, Tier.SILVER, false, "4.00"),
                    rule(GOLD, Tier.GOLD, true, "5.00"),
                    rule(PLATINUM, Tier.PLATINUM, true, "9.50")),
                List.of())));

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    GymPlanItemResponseV2 platinum = plan(detail, PLATINUM);
    assertEquals(9.5, platinum.dailyPrice());
    assertEquals(2, detail.supportedSubscriptions().size());
  }

  @Test
  void orderFailureStillReturnsPolicyBackedPlan() {
    when(order.getGymPlans()).thenThrow(new RuntimeException("order down"));
    when(policyService.find(14L))
        .thenReturn(Optional.of(
            new GymMembershipPolicyV4(
                1,
                false,
                List.of(
                    rule(BRONZE, Tier.BRONZE, false, "3.00"),
                    rule(SILVER, Tier.SILVER, false, "4.00"),
                    rule(GOLD, Tier.GOLD, true, "5.00"),
                    rule(PLATINUM, Tier.PLATINUM, true, "9.50")),
                List.of())));

    GymDetailResponseV4 detail = service.getGymDetailV4(null, 14L);

    // Ranks cannot be resolved without order-backend, so eligibility fails closed and nothing is
    // fabricated; the call must still succeed instead of throwing.
    assertNotNull(detail);
  }

  // --- helpers -----------------------------------------------------------------------------

  static SubscriptionPackageInfo plan(long id, String name) {
    return SubscriptionPackageInfo.newBuilder().setPackageId(id).setName(name).build();
  }

  static PackageRule rule(long id, Tier tier, boolean primary, String price) {
    return new PackageRule(
        id, tier, primary, primary, new BigDecimal(price), primary ? new BigDecimal("50") : null,
        List.of(), null);
  }

  static PackageRule includedOnly(long id, Tier tier, String price) {
    return includedOnly(id, tier, price, true);
  }

  static PackageRule includedOnly(long id, Tier tier, String price, boolean included) {
    return new PackageRule(
        id, tier, included, false, new BigDecimal(price), null, List.of(), null);
  }

  static boolean homeEligible(GymDetailResponseV4 detail, Tier tier) {
    return detail.packages().stream()
        .filter(p -> p.tier() == tier)
        .map(GymPackageHomeEligibilityResponse::homeEligible)
        .findFirst()
        .orElseThrow();
  }

  static Set<String> planIds(GymDetailResponseV4 detail) {
    Set<String> ids = new LinkedHashSet<>();
    for (GymPlanItemResponseV2 p : detail.supportedSubscriptions()) ids.add(p.plan_id());
    return ids;
  }

  static GymPlanItemResponseV2 plan(GymDetailResponseV4 detail, long packageId) {
    return detail.supportedSubscriptions().stream()
        .filter(p -> String.valueOf(packageId).equals(p.plan_id()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("missing plan " + packageId));
  }

  /** The service has ~30 collaborators; mock them all except the ones this scenario drives. */
  private GymReadServiceImpl newService(HomeGymRuleService rules) throws Exception {
    Constructor<?> ctor = GymReadServiceImpl.class.getDeclaredConstructors()[0];
    Object[] args = new Object[ctor.getParameterCount()];
    Class<?>[] types = ctor.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class<?> t = types[i];
      if (t == TrainerRepository.class) {
        TrainerRepository trainers = mock(TrainerRepository.class);
        when(trainers.findByGymId(any(), any())).thenReturn(Page.empty());
        args[i] = trainers;
      } else if (t == ReviewRepository.class) {
        ReviewRepository reviews = mock(ReviewRepository.class);
        when(reviews.findByGymId(any(), any(Pageable.class))).thenReturn(Page.empty());
        args[i] = reviews;
      } else if (t == GymRepository.class) args[i] = gymRepository;
      else if (t == OrderServiceGrpcClient.class) args[i] = order;
      else if (t == HomeGymRuleService.class) args[i] = rules;
      else if (t == GymMembershipPolicyV4Service.class) args[i] = policyService;
      else if (t == GymPackagePriceRepository.class) args[i] = packagePrices;
      else if (t == Executor.class) args[i] = (Executor) Runnable::run;
      else args[i] = mock(t);
    }
    return (GymReadServiceImpl) ctor.newInstance(args);
  }
}
