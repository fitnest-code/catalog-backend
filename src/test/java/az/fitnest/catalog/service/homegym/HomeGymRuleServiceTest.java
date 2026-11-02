package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import az.fitnest.order.grpc.SubscriptionPackageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R1/R14/R15/R17/R18 catalogue rules: package hierarchy, master/package eligibility, the
 * check-in resolution (window, tier, price snapshot) and the displayed available hours.
 */
class HomeGymRuleServiceTest {

    private static final Long GYM_ID = 70L;
    private static final Long BRONZE = 1201L;
    private static final Long SILVER = 1202L;
    private static final Long GOLD = 1203L;
    private static final Long PLATINUM = 1204L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final LocalDateTime NOW = TODAY.atTime(12, 0);

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
    }

    // ------------------------------------------------------------------
    // Package hierarchy (R1)
    // ------------------------------------------------------------------

    @Test
    void packageContextResolvesNameAndLowerOrEqualRanks() {
        HomeGymRuleService.PackageContext context = service.packageContext(GOLD);

        assertEquals(GOLD, context.packageId());
        assertEquals("Gold", context.name());
        assertEquals(3, context.rank());
        assertEquals(Set.of(BRONZE, SILVER, GOLD), Set.copyOf(context.eligiblePackageIds()));
        assertFalse(context.eligiblePackageIds().contains(PLATINUM));
    }

    @Test
    void packageContextFallsBackToTheNameEndpointWhenThePlansMissThePackage() {
        when(orderServiceGrpcClient.getGymPlans()).thenReturn(List.of(plan(BRONZE, "Bronze")));
        when(orderServiceGrpcClient.getPackageNamesByIds(List.of(SILVER)))
                .thenReturn(List.of(name(SILVER, "Silver")));

        HomeGymRuleService.PackageContext context = service.packageContext(SILVER);

        assertEquals("Silver", context.name());
        assertEquals(2, context.rank());
        assertTrue(context.eligiblePackageIds().contains(SILVER));
        assertTrue(context.eligiblePackageIds().contains(BRONZE));
        assertFalse(context.eligiblePackageIds().contains(GOLD));
    }

    @Test
    void packageContextTreatsAGrpcFailureAsUnknownAndDoesNotCacheIt() {
        when(orderServiceGrpcClient.getGymPlans()).thenThrow(new IllegalStateException("down"));

        HomeGymRuleService.PackageContext first = service.packageContext(GOLD);
        HomeGymRuleService.PackageContext second = service.packageContext(GOLD);

        assertEquals(0, first.rank());
        assertNull(first.name());
        assertEquals(List.of(GOLD), first.eligiblePackageIds());
        assertEquals(first, second);
        // A transient failure must be retried instead of being cached for 10 minutes.
        verify(orderServiceGrpcClient, times(2)).getGymPlans();
    }

    @Test
    void packageContextOfNullIsEmpty() {
        HomeGymRuleService.PackageContext context = service.packageContext(null);

        assertNull(context.packageId());
        assertNull(context.name());
        assertEquals(0, context.rank());
        assertTrue(context.eligiblePackageIds().isEmpty());
        verify(orderServiceGrpcClient, never()).getGymPlans();
    }

    @Test
    void eligiblePackageIdsNeverReturnsAnEmptyList() {
        when(orderServiceGrpcClient.getGymPlans()).thenThrow(new IllegalStateException("down"));

        assertEquals(List.of(GOLD), service.eligiblePackageIds(GOLD));
        assertEquals(List.of(-1L), service.eligiblePackageIds(null));
    }

    // ------------------------------------------------------------------
    // R18 master switch and package override
    // ------------------------------------------------------------------

    @Test
    void masteredSwitchOffBeatsThePackagePrice() {
        Gym gym = gym(GYM_ID, false, GOLD);

        assertFalse(service.isHomeEligible(gym, GOLD));
        verify(packagePriceRepository, never())
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        anyLong(), anyLong(), any(LocalDate.class));
    }

    @Test
    void aGymWithoutAPriceRowIsEligible() {
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        GYM_ID, GOLD, LocalDate.now(HomeGymRuleService.BAKU)))
                .thenReturn(Optional.empty());

        assertTrue(service.isHomeEligible(gym(GYM_ID, true, GOLD), GOLD));
    }

    @Test
    void packageSwitchOffDisablesEligibility() {
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        GYM_ID, GOLD, LocalDate.now(HomeGymRuleService.BAKU)))
                .thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, false)));

        assertFalse(service.isHomeEligible(gym(GYM_ID, true, GOLD), GOLD));
    }

    @Test
    void packageSwitchOnKeepsEligibility() {
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        GYM_ID, GOLD, LocalDate.now(HomeGymRuleService.BAKU)))
                .thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, true)));

        assertTrue(service.isHomeEligible(gym(GYM_ID, true, GOLD), GOLD));
    }

    // ------------------------------------------------------------------
    // Check-in resolution (R14/R15)
    // ------------------------------------------------------------------

    @Test
    void withoutRulesTheEntryIsAllowedAtTheSubscriberPackagePrice() {
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of());
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertTrue(resolution.allowed());
        assertNull(resolution.reason());
        assertNull(resolution.ruleId());
        assertEquals("Gold", resolution.pricePackageName());
        assertEquals(10.0, resolution.singlePrice());
        assertEquals(100.0, resolution.unlimitedPrice());
        assertNull(resolution.availableHours());
    }

    @Test
    void aScanOutsideTheWindowIsRejected() {
        GymTimeSlotRule rule = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(11, 0), Set.of(GOLD), GOLD);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of(rule));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertFalse(resolution.allowed());
        assertEquals("OUTSIDE_HOURS", resolution.reason());
        assertEquals("09:00-11:00", resolution.availableHours());
    }

    @Test
    void aWindowThatExcludesThePackageIsRejectedAsTier() {
        GymTimeSlotRule rule = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(PLATINUM), PLATINUM);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of(rule));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertFalse(resolution.allowed());
        assertEquals("TIER", resolution.reason());
        assertNull(resolution.availableHours());
    }

    @Test
    void aMatchingRuleUsesItsOwnPricePackage() {
        GymTimeSlotRule rule = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(GOLD), SILVER);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of(rule));
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(SILVER), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, SILVER, 8.0, 80.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertTrue(resolution.allowed());
        assertEquals(1L, resolution.ruleId());
        assertEquals("Silver", resolution.pricePackageName());
        assertEquals(8.0, resolution.singlePrice());
        assertEquals(80.0, resolution.unlimitedPrice());
    }

    @Test
    void theNewestMatchingRuleWins() {
        GymTimeSlotRule older = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(GOLD), GOLD);
        older.setEffectiveFrom(TODAY.minusMonths(1));
        GymTimeSlotRule newer = rule(2L, "MONDAY", LocalTime.of(10, 0), LocalTime.of(18, 0),
                Set.of(GOLD), GOLD);
        newer.setEffectiveFrom(TODAY);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY))
                .thenReturn(List.of(newer, older));
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertEquals(2L, resolution.ruleId());
    }

    @Test
    void anExplicitPackageRuleWinsOverAGenericRule() {
        GymTimeSlotRule generic = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(), GOLD);
        generic.setEffectiveFrom(TODAY);
        GymTimeSlotRule explicit = rule(2L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(GOLD), GOLD);
        explicit.setEffectiveFrom(TODAY.minusMonths(2));
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY))
                .thenReturn(List.of(generic, explicit));
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertEquals(2L, resolution.ruleId());
    }

    @Test
    void aRuleWithoutAPricePackageFallsBackToTheSubscriberPackage() {
        GymTimeSlotRule rule = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(GOLD), null);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of(rule));
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertEquals("Gold", resolution.pricePackageName());
        assertEquals(10.0, resolution.singlePrice());
    }

    @Test
    void missingPriceRowFallsBackToTheGymDailyPrice() {
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of());
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.empty());
        Gym gym = gym(GYM_ID, true, GOLD);
        gym.getSubscriptions().iterator().next().setDailyPrice(15.0);

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym, service.packageContext(GOLD), NOW);

        assertEquals(15.0, resolution.singlePrice());
        assertEquals(15.0, resolution.unlimitedPrice());
        assertEquals("Gold", resolution.pricePackageName());
    }

    @Test
    void withoutAnyPriceTheSnapshotIsZeroInsteadOfNull() {
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of());
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.empty());

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertEquals(0.0, resolution.singlePrice());
        assertEquals(0.0, resolution.unlimitedPrice());
    }

    @Test
    void unknownWeekdayNamesNeverMatch() {
        GymTimeSlotRule rule = rule(1L, "HOLIDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(GOLD), GOLD);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of(rule));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertFalse(resolution.allowed());
        assertEquals("OUTSIDE_HOURS", resolution.reason());
    }

    @Test
    void weekdayNamesAreCaseInsensitiveAndTolerateSpaces() {
        GymTimeSlotRule rule = rule(1L, " monday , TUESDAY ", LocalTime.of(9, 0),
                LocalTime.of(18, 0), Set.of(GOLD), GOLD);
        when(timeSlotRuleRepository.findEffectiveRules(GYM_ID, TODAY)).thenReturn(List.of(rule));
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, GOLD, 10.0, 100.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertTrue(resolution.allowed());
        assertEquals(1L, resolution.ruleId());
    }

    // ------------------------------------------------------------------
    // Available hours shown in the list (screen 1.3)
    // ------------------------------------------------------------------

    @Test
    void availableHoursIsNullWithoutRules() {
        when(timeSlotRuleRepository.findEffectiveRules(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of());

        assertNull(service.availableHours(gym(GYM_ID, true, GOLD), service.packageContext(GOLD)));
    }

    @Test
    void availableHoursJoinsDistinctAllowedWindows() {
        GymTimeSlotRule morning = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(12, 0),
                Set.of(GOLD), GOLD);
        GymTimeSlotRule morningDuplicate = rule(2L, "TUESDAY", LocalTime.of(9, 0), LocalTime.of(12, 0),
                Set.of(GOLD), GOLD);
        GymTimeSlotRule evening = rule(3L, "MONDAY", LocalTime.of(18, 0), LocalTime.of(20, 0),
                Set.of(GOLD), GOLD);
        when(timeSlotRuleRepository.findEffectiveRules(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(morning, morningDuplicate, evening));

        assertEquals("09:00-12:00, 18:00-20:00",
                service.availableHours(gym(GYM_ID, true, GOLD), service.packageContext(GOLD)));
    }

    @Test
    void availableHoursIsNullWhenAnAllowedRuleCoversEveryDayAllDay() {
        GymTimeSlotRule allDay = rule(1L, null, null, null, Set.of(GOLD), GOLD);
        when(timeSlotRuleRepository.findEffectiveRules(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(allDay));

        assertNull(service.availableHours(gym(GYM_ID, true, GOLD), service.packageContext(GOLD)));
    }

    @Test
    void availableHoursIsNullWhenNoRuleAllowsThePackage() {
        GymTimeSlotRule platinumOnly = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(PLATINUM), PLATINUM);
        when(timeSlotRuleRepository.findEffectiveRules(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(platinumOnly));

        assertNull(service.availableHours(gym(GYM_ID, true, GOLD), service.packageContext(GOLD)));
    }

    @Test
    void aHigherRankPackageInheritsAccessToAnAllowedLowerRankWindow() {
        GymTimeSlotRule bronzeOnly = rule(1L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(18, 0),
                Set.of(BRONZE), BRONZE);
        when(timeSlotRuleRepository.findEffectiveRules(anyLong(), any(LocalDate.class)))
                .thenReturn(List.of(bronzeOnly));
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(BRONZE), any(LocalDate.class))).thenReturn(Optional.of(price(GYM_ID, BRONZE, 5.0, 50.0, true)));

        HomeGymRuleService.CheckInResolution resolution =
                service.resolveForCheckIn(gym(GYM_ID, true, GOLD), service.packageContext(GOLD), NOW);

        assertTrue(resolution.allowed());
        assertEquals("Bronze", resolution.pricePackageName());
        assertEquals(5.0, resolution.singlePrice());
    }

    @Test
    void supportsPackageUsesTheEligibleHierarchy() {
        HomeGymRuleService.PackageContext goldContext = service.packageContext(GOLD);

        assertTrue(service.supportsPackage(gym(GYM_ID, true, SILVER), goldContext));
        assertFalse(service.supportsPackage(gym(GYM_ID, true, PLATINUM), goldContext));
        assertFalse(service.supportsPackage(null, goldContext));
        assertFalse(service.supportsPackage(gym(GYM_ID, true, GOLD), null));
    }

    @Test
    void supportsPackageIsFalseForAGymWithoutSubscriptions() {
        Gym gym = gymReference(GYM_ID);
        gym.setName("FitZone");

        assertFalse(service.supportsPackage(gym, service.packageContext(GOLD)));
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

    private static az.fitnest.order.grpc.PackageNameInfo name(long id, String name) {
        return az.fitnest.order.grpc.PackageNameInfo.newBuilder().setPackageId(id).setName(name).build();
    }

    private static Gym gym(Long id, boolean homeEligible, Long packageId) {
        GymSubscription subscription = new GymSubscription();
        subscription.setPackageId(packageId);
        Gym gym = Gym.builder()
                .name("FitZone")
                .homeEligible(homeEligible)
                .subscriptions(new java.util.HashSet<>(Set.of(subscription)))
                .build();
        gym.setId(id);
        return gym;
    }

    private static Gym gymReference(Long id) {
        Gym gym = new Gym();
        gym.setId(id);
        return gym;
    }

    private static GymPackagePrice price(Long gymId, Long packageId, Double single,
                                         Double unlimited, Boolean homeEligible) {
        return GymPackagePrice.builder()
                .gym(gymReference(gymId))
                .packageId(packageId)
                .singlePrice(single)
                .unlimitedPrice(unlimited)
                .homeEligible(homeEligible)
                .included(true)
                .effectiveFrom(TODAY.minusMonths(1))
                .build();
    }

    private static GymTimeSlotRule rule(Long id, String weekdays, LocalTime start, LocalTime end,
                                        Set<Long> allowedPackages, Long pricePackageId) {
        GymTimeSlotRule rule = new GymTimeSlotRule();
        rule.setId(id);
        rule.setGym(gymReference(GYM_ID));
        rule.setWeekdays(weekdays);
        rule.setStartTime(start);
        rule.setEndTime(end);
        rule.setAllowedPackageIds(new java.util.LinkedHashSet<>(allowedPackages));
        rule.setPricePackageId(pricePackageId);
        rule.setEffectiveFrom(TODAY.minusMonths(1));
        return rule;
    }
}
