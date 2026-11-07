package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.Tier;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import az.fitnest.order.grpc.SubscriptionPackageInfo;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test scenarios for the read-only home-gym hierarchy introduced for pre-purchase
 * list/detail/search endpoints.
 *
 * <p>These tests cover the rule boundaries, batch path behavior, and compatibility
 * with the post-purchase home-gym list that uses the same minTier rule.
 */
class HomeGymReadOnlyScenarioTest {

    private static final Long GYM_ID = 700L;
    private static final Long BRONZE = 1201L;
    private static final Long SILVER = 1202L;
    private static final Long GOLD = 1203L;
    private static final Long PLATINUM = 1204L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private GymPackagePriceRepository packagePriceRepository;
    private GymTimeSlotRuleRepository timeSlotRuleRepository;
    private OrderServiceGrpcClient orderServiceGrpcClient;
    private HomeGymRuleService service;

    @BeforeEach
    void setUp() throws Exception {
        packagePriceRepository = mock(GymPackagePriceRepository.class);
        timeSlotRuleRepository = mock(GymTimeSlotRuleRepository.class);
        orderServiceGrpcClient = mock(OrderServiceGrpcClient.class);
        service = new HomeGymRuleService(packagePriceRepository, timeSlotRuleRepository,
                orderServiceGrpcClient);
        clearPackageContextCache();

        when(orderServiceGrpcClient.getGymPlans()).thenReturn(List.of(
                plan(BRONZE, "Bronze"), plan(SILVER, "Silver"),
                plan(GOLD, "Gold"), plan(PLATINUM, "Platinum")));
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of());
    }

    // ------------------------------------------------------------------
    // Master switch and absent data
    // ------------------------------------------------------------------

    @Test
    void masterSwitchOffYieldsAnEmptyHierarchy() {
        Gym gym = gym(GYM_ID, false, BRONZE, SILVER, GOLD, PLATINUM);

        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym);

        assertNull(info.minTier());
        assertTrue(info.tiers().isEmpty());
        assertFalse(info.eligible());
        assertNull(info.eligibleForRequestedTier());
    }

    @Test
    void isHomeEligibleDoesNotEvenReadPricesWhenTheMasterSwitchIsOff() {
        Gym gym = gym(GYM_ID, false, GOLD);

        assertFalse(service.isHomeEligible(gym, GOLD));
        verify(packagePriceRepository, never()).findLatestByGymIdIn(any(), any(LocalDate.class));
    }

    @Test
    void aGymWithoutOfferedPackagesIsNotEligible() {
        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true));

        assertNull(info.minTier());
        assertTrue(info.tiers().isEmpty());
        assertFalse(info.eligible());
    }

    @Test
    void aNullGymOrGymIdIsNotEligible() {
        assertFalse(service.homeGymInfo(null).eligible());
        assertFalse(service.homeGymInfo(gymReference(null)).eligible());
        assertFalse(service.isHomeEligible(null, GOLD));
    }

    // ------------------------------------------------------------------
    // Hierarchy boundaries - the upward-closed rule
    // ------------------------------------------------------------------

    @Test
    void allOfferedTiersEnabledStartsAtTheLowestTier() {
        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM));

        assertEquals(Tier.BRONZE, info.minTier());
        assertEquals(List.of(Tier.BRONZE, Tier.SILVER, Tier.GOLD, Tier.PLATINUM), info.tiers());
        assertTrue(info.eligible());
    }

    @Test
    void onlyTheHighestOfferedTierEnabledStartsThere() {
        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, PLATINUM));

        assertEquals(Tier.PLATINUM, info.minTier());
        assertEquals(List.of(Tier.PLATINUM), info.tiers());
    }

    @Test
    void aMissingPriceRowIsTreatedAsEnabled() {
        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, GOLD));

        assertEquals(Tier.GOLD, info.minTier());
        assertEquals(List.of(Tier.GOLD, Tier.PLATINUM), info.tiers());
    }

    @Test
    void includedFalseDisablesThatTier() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, BRONZE, true, false)));

        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM));

        assertEquals(Tier.SILVER, info.minTier());
        assertEquals(List.of(Tier.SILVER, Tier.GOLD, Tier.PLATINUM), info.tiers());
    }

    @Test
    void homeEligibleFalseDisablesThatTier() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, SILVER, false, true)));

        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM));

        assertEquals(Tier.GOLD, info.minTier());
        assertEquals(List.of(Tier.GOLD, Tier.PLATINUM), info.tiers());
    }

    @Test
    void aNullHomeEligibleFlagKeepsTheTierEnabled() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, BRONZE, null, true)));

        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER));

        assertEquals(Tier.BRONZE, info.minTier());
    }

    @Test
    void theHighestOfferedTierBeingDisabledMakesTheGymIneligible() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, GOLD, false, true)));

        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, BRONZE, GOLD));

        assertNull(info.minTier());
        assertTrue(info.tiers().isEmpty());
        assertFalse(info.eligible());
    }

    @Test
    void aDisabledMiddleTierRaisesTheMinimumToTheNextOfferedTier() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, SILVER, false, true)));

        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM));

        assertEquals(Tier.GOLD, info.minTier());
        assertEquals(List.of(Tier.GOLD, Tier.PLATINUM), info.tiers());
    }

    @Test
    void aDisabledLowerTierDoesNotAffectTheHigherTiers() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, BRONZE, false, true)));

        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM));

        assertEquals(Tier.SILVER, info.minTier());
        assertEquals(List.of(Tier.SILVER, Tier.GOLD, Tier.PLATINUM), info.tiers());
    }

    @Test
    void anUnknownPackageTierCannotMakeAGymEligible() {
        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, 9999L));

        assertNull(info.minTier());
        assertFalse(info.eligible());
    }

    @Test
    void anUnknownTierDoesNotHideAKnownTier() {
        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, 9999L, GOLD));

        assertEquals(Tier.GOLD, info.minTier());
    }

    // ------------------------------------------------------------------
    // Requested tier eligibility
    // ------------------------------------------------------------------

    @Test
    void withoutARequestedTierTheEligibilityFlagIsAbsent() {
        HomeGymRuleService.HomeGymInfo info = service.homeGymInfo(gym(GYM_ID, true, GOLD));

        assertNull(info.eligibleForRequestedTier());
    }

    @Test
    void aRequestedTierAtOrAboveTheMinimumIsEligible() {
        HomeGymRuleService.HomeGymInfo gold =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM), 3);
        HomeGymRuleService.HomeGymInfo platinum =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD, PLATINUM), 4);

        assertTrue(gold.eligibleForRequestedTier());
        assertTrue(platinum.eligibleForRequestedTier());
    }

    @Test
    void silverOnlyGymIsEligibleForGoldViaPackageAndAboveHierarchy() {
        // Package-and-above: a gym that offers SILVER (minTier=SILVER) is selectable
        // for GOLD even when GOLD is not listed on the gym's subscriptions.
        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, SILVER), 3);

        assertEquals(Tier.SILVER, info.minTier());
        assertTrue(info.eligibleForRequestedTier());
        assertTrue(service.isHomeEligible(gym(GYM_ID, true, SILVER), GOLD));
    }

    @Test
    void gymOfferingOnlyHigherTiersIsNotEligibleForALowerRequestedTier() {
        // PLATINUM-only gym cannot be home for a GOLD request (requestedRank < minRank).
        HomeGymRuleService.HomeGymInfo info =
                service.homeGymInfo(gym(GYM_ID, true, PLATINUM), 3);

        assertEquals(Tier.PLATINUM, info.minTier());
        assertFalse(info.eligibleForRequestedTier());
        assertFalse(service.isHomeEligible(gym(GYM_ID, true, PLATINUM), GOLD));
    }

    @Test
    void aRequestedTierBelowTheMinimumIsNotEligible() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, BRONZE, false, true)));

        HomeGymRuleService.HomeGymInfo bronze =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD), 1);
        // BRONZE is disabled, so minTier = SILVER (rank 2)
        // requestedRank 1 < minRank 2 → not eligible
        // silver with requestedRank 2: 2 >= 2 → eligible
        // But the test checks that requesting the same tier as minTier is eligible

        HomeGymRuleService.HomeGymInfo silver =
                service.homeGymInfo(gym(GYM_ID, true, BRONZE, SILVER, GOLD), 2);

        assertEquals(Tier.SILVER, bronze.minTier());
        assertFalse(bronze.eligibleForRequestedTier());
        // requestedRank 2 >= minRank 2 → eligible
        assertTrue(silver.eligibleForRequestedTier());
    }

    // ------------------------------------------------------------------
    // Batch path used by the list endpoints
    // ------------------------------------------------------------------

    @Test
    void theBatchPathResolvesEveryGymWithASinglePriceQuery() {
        Gym first = gym(GYM_ID, true, BRONZE, SILVER);
        Gym second = gym(701L, true, GOLD, PLATINUM);

        when(packagePriceRepository.findLatestByGymIdIn(eq(List.of(GYM_ID, 701L)),
                any(LocalDate.class))).thenReturn(List.of(
                price(GYM_ID, BRONZE, false, true),
                price(701L, GOLD, false, true)));

        Map<Long, HomeGymRuleService.HomeGymInfo> result =
                service.homeGymInfoByGym(List.of(first, second));

        assertEquals(2, result.size());
        // BRONZE disabled, next offered is SILVER
        assertEquals(Tier.SILVER, result.get(GYM_ID).minTier());
        // GOLD disabled, next offered is PLATINUM
        assertEquals(Tier.PLATINUM, result.get(701L).minTier());
        verify(packagePriceRepository, times(1))
                .findLatestByGymIdIn(eq(List.of(GYM_ID, 701L)), any(LocalDate.class));
    }

    @Test
    void theBatchPathReflectsTheRequestedTierForEveryGym() {
        Gym first = gym(GYM_ID, true, GOLD);
        Gym second = gym(701L, true, BRONZE);

        Map<Long, HomeGymRuleService.HomeGymInfo> result =
                service.homeGymInfoByGym(List.of(first, second), 3);

        assertEquals(Tier.GOLD, result.get(GYM_ID).minTier());
        assertTrue(result.get(GYM_ID).eligibleForRequestedTier());
        assertEquals(Tier.BRONZE, result.get(701L).minTier());
        assertTrue(result.get(701L).eligibleForRequestedTier());
    }

    @Test
    void theBatchPathSkipsNullGymsAndNullIds() {
        Gym withoutId = new Gym();
        Map<Long, HomeGymRuleService.HomeGymInfo> result =
                service.homeGymInfoByGym(Arrays.asList(null, withoutId));

        assertTrue(result.isEmpty());
        verify(packagePriceRepository, never()).findLatestByGymIdIn(any(), any(LocalDate.class));
    }

    @Test
    void anEmptyBatchDoesNotQueryTheDatabase() {
        assertTrue(service.homeGymInfoByGym(List.of()).isEmpty());
        assertTrue(service.homeGymInfoByGym(null).isEmpty());
        verify(packagePriceRepository, never()).findLatestByGymIdIn(any(), any(LocalDate.class));
    }

    // ------------------------------------------------------------------
    // Legacy entry point (post-purchase home-gym list and gRPC analytics)
    // ------------------------------------------------------------------

    @Test
    void isHomeEligibleWithoutAPackageIdUsesAnyEnabledTier() {
        assertTrue(service.isHomeEligible(gym(GYM_ID, true, BRONZE), null));
    }

    @Test
    void isHomeEligibleRejectsAnExplicitlyDisabledPackage() {
        when(packagePriceRepository.findLatestByGymIdIn(any(), any(LocalDate.class)))
                .thenReturn(List.of(price(GYM_ID, BRONZE, false, true)));

        assertFalse(service.isHomeEligible(gym(GYM_ID, true, BRONZE), BRONZE));
        assertTrue(service.isHomeEligible(gym(GYM_ID, true, BRONZE, SILVER), SILVER));
    }

    @Test
    void tierRankIsOneBasedAndNullSafe() {
        assertEquals(0, HomeGymRuleService.tierRank(null));
        assertEquals(1, HomeGymRuleService.tierRank(Tier.BRONZE));
        assertEquals(2, HomeGymRuleService.tierRank(Tier.SILVER));
        assertEquals(3, HomeGymRuleService.tierRank(Tier.GOLD));
        assertEquals(4, HomeGymRuleService.tierRank(Tier.PLATINUM));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void clearPackageContextCache() throws Exception {
        Field field = HomeGymRuleService.class.getDeclaredField("PACKAGE_CONTEXT_CACHE");
        field.setAccessible(true);
        ((ConcurrentHashMap<Long, ?>) field.get(null)).clear();
    }

    private static SubscriptionPackageInfo plan(long id, String name) {
        return SubscriptionPackageInfo.newBuilder().setPackageId(id).setName(name).build();
    }

    private static Gym gym(Long id, boolean homeEligible, Long... packageIds) {
        Set<GymSubscription> subscriptions = new LinkedHashSet<>();
        for (Long packageId : packageIds) {
            GymSubscription subscription = new GymSubscription();
            subscription.setPackageId(packageId);
            subscriptions.add(subscription);
        }
        Gym gym = Gym.builder()
                .name("FitZone")
                .homeEligible(homeEligible)
                .subscriptions(subscriptions)
                .build();
        gym.setId(id);
        return gym;
    }

    private static Gym gymReference(Long id) {
        Gym gym = new Gym();
        gym.setId(id);
        return gym;
    }

    private static GymPackagePrice price(Long gymId, Long packageId, Boolean homeEligible,
                                         Boolean included) {
        return GymPackagePrice.builder()
                .gym(gymReference(gymId))
                .packageId(packageId)
                .singlePrice(10.0)
                .unlimitedPrice(100.0)
                .homeEligible(homeEligible)
                .included(included)
                .effectiveFrom(TODAY.minusMonths(1))
                .build();
    }
}