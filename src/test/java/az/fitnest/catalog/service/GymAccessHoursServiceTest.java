package az.fitnest.catalog.service;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.response.GymAccessHoursResponse;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import az.fitnest.catalog.service.homegym.HomeGymRuleService.PackageContext;
import az.fitnest.order.grpc.ActiveSubscriptionResponse;
import az.fitnest.order.grpc.SubscriptionPackageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GymAccessHoursServiceTest {

    private static final Long GYM_ID = 50L;
    private static final Long USER_ID = 7L;
    private static final Long BRONZE = 1L;
    private static final Long SILVER = 2L;
    private static final Long GOLD = 3L;
    private static final Long PLATINUM = 4L;

    private GymRepository gymRepository;
    private GymTimeSlotRuleRepository timeSlotRuleRepository;
    private HomeGymRuleService homeGymRuleService;
    private OrderServiceGrpcClient orderServiceGrpcClient;
    private GymAccessHoursService service;
    private Gym gym;

    @BeforeEach
    void setUp() {
        gymRepository = mock(GymRepository.class);
        timeSlotRuleRepository = mock(GymTimeSlotRuleRepository.class);
        homeGymRuleService = mock(HomeGymRuleService.class);
        orderServiceGrpcClient = mock(OrderServiceGrpcClient.class);
        service = new GymAccessHoursService(
                gymRepository, timeSlotRuleRepository, homeGymRuleService, orderServiceGrpcClient);

        gym = new Gym();
        gym.setId(GYM_ID);
        gym.setName("Test Gym");
        gym.setGeneralWorkHours(new HashSet<>(List.of(
                workHour(GymWorkHourPeriod.MONDAY, "07:00", "22:00"),
                workHour(GymWorkHourPeriod.TUESDAY, "07:00", "22:00"),
                workHour(GymWorkHourPeriod.WEDNESDAY, "07:00", "22:00"),
                workHour(GymWorkHourPeriod.THURSDAY, "07:00", "22:00"),
                workHour(GymWorkHourPeriod.FRIDAY, "07:00", "22:00"),
                workHour(GymWorkHourPeriod.SATURDAY, "09:00", "18:00")
        )));
        gym.setRestDays(EnumSet.of(GymWorkHourPeriod.SUNDAY));

        when(gymRepository.findAccessHoursById(GYM_ID)).thenReturn(Optional.of(gym));
        stubPackages();
    }

    @Test
    void unrestrictedGym_openDuringWorkHours() {
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of());
        stubActiveBronze();

        LocalDateTime thursdayNoon = LocalDateTime.of(2026, 11, 12, 12, 0);
        GymAccessHoursResponse response = service.getAccessHours(GYM_ID, USER_ID, thursdayNoon);

        assertFalse(response.isHasPackageRestrictions());
        assertTrue(response.getByPackage().isEmpty());
        assertNotNull(response.getForUser());
        assertEquals(BRONZE, response.getForUser().packageId);
        assertTrue(response.getForUser().openNow);
        assertEquals(LocalDateTime.of(2026, 11, 12, 22, 0), response.getForUser().closesAt);
        assertTrue(response.getForUser().minutesLeft > 0);
    }

    @Test
    void restDay_closedWithNextOpenAt() {
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of());
        stubActiveBronze();

        LocalDateTime sundayNoon = LocalDateTime.of(2026, 11, 15, 12, 0);
        GymAccessHoursResponse response = service.getAccessHours(GYM_ID, USER_ID, sundayNoon);

        assertNotNull(response.getForUser());
        assertFalse(response.getForUser().openNow);
        assertNull(response.getForUser().closesAt);
        assertEquals(LocalDateTime.of(2026, 11, 16, 7, 0), response.getForUser().nextOpenAt);
    }

    @Test
    void bronzeInsideLimitedWindow_openNow() {
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(bronzeLimitedRules());
        stubActiveBronze();
        stubGymPlans();

        LocalDateTime thursday1130 = LocalDateTime.of(2026, 11, 12, 11, 30);
        GymAccessHoursResponse response = service.getAccessHours(GYM_ID, USER_ID, thursday1130);

        assertTrue(response.isHasPackageRestrictions());
        assertNotNull(response.getForUser());
        assertTrue(response.getForUser().openNow);
        assertEquals(LocalDateTime.of(2026, 11, 12, 14, 0), response.getForUser().closesAt);
        assertEquals(150, response.getForUser().minutesLeft);
    }

    @Test
    void bronzeOutsideLimitedWindow_closedWhileSilverWouldBeOpen() {
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(bronzeLimitedRules());
        stubActiveBronze();
        stubGymPlans();

        LocalDateTime thursday1800 = LocalDateTime.of(2026, 11, 12, 18, 0);
        GymAccessHoursResponse bronzeResponse = service.getAccessHours(GYM_ID, USER_ID, thursday1800);
        assertFalse(bronzeResponse.getForUser().openNow);
        assertEquals(LocalDateTime.of(2026, 11, 13, 10, 0), bronzeResponse.getForUser().nextOpenAt);

        when(orderServiceGrpcClient.getActiveSubscription(USER_ID)).thenReturn(
                ActiveSubscriptionResponse.newBuilder()
                        .setSubscriptionStatus("ACTIVE")
                        .setPackageId(SILVER)
                        .setPackageName("Silver")
                        .build());
        GymAccessHoursResponse silverResponse = service.getAccessHours(GYM_ID, USER_ID, thursday1800);
        assertTrue(silverResponse.getForUser().openNow);
        assertEquals(LocalDateTime.of(2026, 11, 12, 22, 0), silverResponse.getForUser().closesAt);
    }

    @Test
    void byPackage_bronzeLimitedWithDayTaggedWindows_higherTiersFullHours() {
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(bronzeLimitedRules());
        stubActiveBronze();
        stubGymPlans();

        LocalDateTime thursdayNoon = LocalDateTime.of(2026, 11, 12, 12, 0);
        GymAccessHoursResponse response = service.getAccessHours(GYM_ID, USER_ID, thursdayNoon);

        GymAccessHoursResponse.GymPackageAccess bronze = packageAccess(response, BRONZE);
        assertNotNull(bronze);
        assertFalse(bronze.fullHours);
        assertFalse(bronze.windows.isEmpty());
        assertTrue(bronze.windows.stream().allMatch(w -> w.dayName != null && !w.dayName.isBlank()));
        assertTrue(bronze.windows.stream().anyMatch(w ->
                "MONDAY".equals(w.dayName)
                        && LocalTime.of(10, 0).equals(w.from)
                        && LocalTime.of(14, 0).equals(w.to)));
        assertTrue(bronze.windows.stream().anyMatch(w ->
                "SATURDAY".equals(w.dayName)
                        && LocalTime.of(10, 0).equals(w.from)
                        && LocalTime.of(14, 0).equals(w.to)));
        // Same clock range on different days must not collapse into duplicate day-less rows.
        long monday1014 = bronze.windows.stream()
                .filter(w -> "MONDAY".equals(w.dayName)
                        && LocalTime.of(10, 0).equals(w.from)
                        && LocalTime.of(14, 0).equals(w.to))
                .count();
        assertEquals(1, monday1014);

        for (Long higher : List.of(SILVER, GOLD, PLATINUM)) {
            GymAccessHoursResponse.GymPackageAccess pkg = packageAccess(response, higher);
            assertNotNull(pkg, "missing package " + higher);
            assertTrue(pkg.fullHours, "package " + higher + " should have fullHours");
            assertTrue(pkg.windows.isEmpty(), "package " + higher + " should omit windows when fullHours");
        }
    }

    private static GymAccessHoursResponse.GymPackageAccess packageAccess(
            GymAccessHoursResponse response, Long packageId) {
        return response.getByPackage().stream()
                .filter(p -> packageId.equals(p.packageId))
                .findFirst()
                .orElse(null);
    }

    private void stubPackages() {
        when(homeGymRuleService.packageContext(BRONZE))
                .thenReturn(new PackageContext(BRONZE, "Bronze", 1, List.of(BRONZE)));
        when(homeGymRuleService.packageContext(SILVER))
                .thenReturn(new PackageContext(SILVER, "Silver", 2, List.of(BRONZE, SILVER)));
        when(homeGymRuleService.packageContext(GOLD))
                .thenReturn(new PackageContext(GOLD, "Gold", 3, List.of(BRONZE, SILVER, GOLD)));
        when(homeGymRuleService.packageContext(PLATINUM))
                .thenReturn(new PackageContext(PLATINUM, "Platinum", 4,
                        List.of(BRONZE, SILVER, GOLD, PLATINUM)));
    }

    private void stubActiveBronze() {
        when(orderServiceGrpcClient.getActiveSubscription(USER_ID)).thenReturn(
                ActiveSubscriptionResponse.newBuilder()
                        .setSubscriptionStatus("ACTIVE")
                        .setPackageId(BRONZE)
                        .setPackageName("Bronze")
                        .build());
    }

    private void stubGymPlans() {
        when(orderServiceGrpcClient.getGymPlans()).thenReturn(List.of(
                SubscriptionPackageInfo.newBuilder().setPackageId(BRONZE).setName("Bronze").build(),
                SubscriptionPackageInfo.newBuilder().setPackageId(SILVER).setName("Silver").build(),
                SubscriptionPackageInfo.newBuilder().setPackageId(GOLD).setName("Gold").build(),
                SubscriptionPackageInfo.newBuilder().setPackageId(PLATINUM).setName("Platinum").build()
        ));
    }

    private List<GymTimeSlotRule> bronzeLimitedRules() {
        String weekdays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
        LocalDate from = LocalDate.of(2026, 11, 1);
        return List.of(
                rule(weekdays, "10:00", "14:00", Set.of(BRONZE), from),
                rule(weekdays, "07:00", "09:59", Set.of(SILVER), from),
                rule(weekdays, "14:00", "22:00", Set.of(SILVER), from),
                rule("SATURDAY", "10:00", "14:00", Set.of(BRONZE), from),
                rule("SATURDAY", "09:00", "09:59", Set.of(SILVER), from),
                rule("SATURDAY", "14:00", "18:00", Set.of(SILVER), from)
        );
    }

    private static GymTimeSlotRule rule(
            String weekdays, String start, String end, Set<Long> packages, LocalDate effectiveFrom) {
        GymTimeSlotRule rule = new GymTimeSlotRule();
        rule.setWeekdays(weekdays);
        rule.setStartTime(LocalTime.parse(start));
        rule.setEndTime(LocalTime.parse(end));
        rule.setAllowedPackageIds(new HashSet<>(packages));
        rule.setEffectiveFrom(effectiveFrom);
        Gym g = new Gym();
        g.setId(GYM_ID);
        rule.setGym(g);
        return rule;
    }

    private static GymWorkHour workHour(GymWorkHourPeriod period, String from, String to) {
        return GymWorkHour.builder()
                .period(period)
                .fromTime(LocalTime.parse(from))
                .toTime(LocalTime.parse(to))
                .build();
    }
}
