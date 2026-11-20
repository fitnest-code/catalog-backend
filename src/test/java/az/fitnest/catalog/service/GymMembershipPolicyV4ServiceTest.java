package az.fitnest.catalog.service;

import static org.junit.jupiter.api.Assertions.*;

import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.*;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GymMembershipPolicyV4ServiceTest {
  List<PackageRule> packages() {
    return Arrays.stream(Tier.values())
        .map(
            t ->
                new PackageRule(
                    (long) t.ordinal() + 1,
                    t,
                    true,
                    true,
                    new BigDecimal("4.00"),
                    new BigDecimal("50.00"),
                    List.of("Fitness"),
                    null))
        .toList();
  }

  TimeRule rule(String from, String to, Tier price, Set<Tier> allowed) {
    return new TimeRule(
        Set.of(DayOfWeek.MONDAY), LocalTime.parse(from), LocalTime.parse(to), allowed, price);
  }

  @Test
  void bronzeMorningWindowAdmitsSilverAndSilverEveningDeniesBronze() throws Exception {
    var service =
        serviceFor(
            List.of(
                rule("00:00", "16:59", Tier.BRONZE, Set.of(Tier.BRONZE)),
                rule("17:00", "00:00", Tier.SILVER, Set.of(Tier.SILVER))),
            LocalTime.MIDNIGHT);
    var morning = LocalDateTime.of(2026, 10, 26, 10, 0);
    var evening = LocalDateTime.of(2026, 10, 26, 17, 0);
    assertTrue(service.quote(10L, 1L, morning).allowed());
    assertTrue(service.quote(10L, 2L, morning).allowed());
    assertFalse(service.quote(10L, 1L, evening).allowed());
    assertTrue(service.quote(10L, 2L, evening).allowed());
    assertTrue(service.quote(10L, 2L, evening.withHour(22)).allowed());
  }

  @Test
  void midnightEndMeansEndOfDay() {
    assertDoesNotThrow(
        () ->
            GymMembershipPolicyV4Service.validateRules(
                new GymMembershipPolicyV4(
                    0,
                    true,
                    packages(),
                    List.of(
                        rule("00:00", "16:59", Tier.BRONZE, Set.of(Tier.values())),
                        rule("17:00", "00:00", Tier.SILVER, Set.of(Tier.SILVER))))));
    assertEquals(
        LocalTime.of(23, 59),
        GymMembershipPolicyV4Service.effectiveEnd(LocalTime.of(17, 0), LocalTime.MIDNIGHT));
    assertEquals(
        LocalTime.MIDNIGHT,
        GymMembershipPolicyV4Service.effectiveEnd(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
  }

  @Test
  void disabledTimeRulesNeedNoRanges() {
    assertDoesNotThrow(
        () ->
            GymMembershipPolicyV4Service.validateRules(
                new GymMembershipPolicyV4(0, false, packages(), null)));
  }

  @Test
  void adjacentMinutesDoNotOverlap() {
    assertDoesNotThrow(
        () ->
            GymMembershipPolicyV4Service.validateRules(
                new GymMembershipPolicyV4(
                    0,
                    true,
                    packages(),
                    List.of(
                        rule("00:01", "16:59", Tier.BRONZE, Set.of(Tier.values())),
                        rule(
                            "17:00",
                            "23:59",
                            Tier.SILVER,
                            Set.of(Tier.SILVER, Tier.GOLD, Tier.PLATINUM))))));
  }

  @Test
  void equalEndpointsOverlap() {
    assertEquals(
        "OVERLAPPING_TIME_RULES",
        assertThrows(
                ResponseStatusException.class,
                () ->
                    GymMembershipPolicyV4Service.validateRules(
                        new GymMembershipPolicyV4(
                            0,
                            true,
                            packages(),
                            List.of(
                                rule("00:01", "17:00", Tier.BRONZE, Set.of(Tier.values())),
                                rule("17:00", "23:59", Tier.SILVER, Set.of(Tier.SILVER))))))
            .getReason());
  }

  @Test
  void cheaperMemberCannotAccessHigherPriceTier() {
    assertEquals(
        "INVALID_TIME_PRICE_TIER",
        assertThrows(
                ResponseStatusException.class,
                () ->
                    GymMembershipPolicyV4Service.validateRules(
                        new GymMembershipPolicyV4(
                            0,
                            true,
                            packages(),
                            List.of(rule("17:00", "23:59", Tier.SILVER, Set.of(Tier.BRONZE))))))
            .getReason());
  }

  @Test
  void duplicateTierIsRejected() {
    var rules = new ArrayList<>(packages());
    rules.set(3, rules.getFirst());
    assertThrows(
        ResponseStatusException.class,
        () ->
            GymMembershipPolicyV4Service.validateRules(
                new GymMembershipPolicyV4(0, false, rules, List.of())));
  }

  @Test
  void primaryRequiresMonthlyPrice() {
    var rules = new ArrayList<>(packages());
    rules.set(
        0, new PackageRule(1L, Tier.BRONZE, true, true, BigDecimal.ONE, null, List.of(), null));
    assertEquals(
        "PRIMARY_PRICE_REQUIRED",
        assertThrows(
                ResponseStatusException.class,
                () ->
                    GymMembershipPolicyV4Service.validateRules(
                        new GymMembershipPolicyV4(0, false, rules, List.of())))
            .getReason());
  }

  @Test
  void inclusiveEndMinuteWorksWithSeconds() {
    var gym =
        Gym.builder()
            .generalWorkHours(
                Set.of(
                    new GymWorkHour(
                        GymWorkHourPeriod.MONDAY, LocalTime.of(0, 1), LocalTime.of(16, 59))))
            .build();
    assertTrue(
        GymMembershipPolicyV4Service.isWorking(gym, DayOfWeek.MONDAY, LocalTime.of(16, 59, 59)));
    assertFalse(GymMembershipPolicyV4Service.isWorking(gym, DayOfWeek.MONDAY, LocalTime.of(17, 0)));
  }

  @Test
  void overnightHoursExtendIntoNextDay() {
    var gym =
        Gym.builder()
            .generalWorkHours(
                Set.of(
                    new GymWorkHour(
                        GymWorkHourPeriod.MONDAY, LocalTime.of(22, 0), LocalTime.of(2, 0))))
            .build();
    assertTrue(GymMembershipPolicyV4Service.isWorking(gym, DayOfWeek.TUESDAY, LocalTime.of(1, 59)));
    assertFalse(GymMembershipPolicyV4Service.isWorking(gym, DayOfWeek.TUESDAY, LocalTime.of(2, 1)));
  }

  @Test
  void proximityDistanceHasMeterUnits() {
    assertEquals(0, GymEntranceV4Service.distanceMeters(40.4, 49.8, 40.4, 49.8), 0.001);
    assertTrue(GymEntranceV4Service.distanceMeters(40.4, 49.8, 40.41, 49.8) > 1000);
  }

  GymMembershipPolicyV4Service serviceFor(List<TimeRule> rules, LocalTime workStart)
      throws Exception {
    var repo =
        org.mockito.Mockito.mock(
            az.fitnest.catalog.repository.GymMembershipPolicyV4Repository.class);
    var gyms = org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymRepository.class);
    var row = new GymMembershipPolicyV4Entity();
    row.setGymId(10L);
    row.setPayload(
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .writeValueAsString(new GymMembershipPolicyV4(0, true, packages(), rules)));
    var gym =
        Gym.builder()
            .name("Test Gym")
            .status(az.fitnest.catalog.model.enums.GymStatus.ACTIVE)
            .generalWorkHours(
                Set.of(
                    new GymWorkHour(GymWorkHourPeriod.MONDAY, workStart, LocalTime.of(23, 59))))
            .build();
    org.mockito.Mockito.when(repo.findById(10L)).thenReturn(Optional.of(row));
    org.mockito.Mockito.when(gyms.findById(10L)).thenReturn(Optional.of(gym));
    return new GymMembershipPolicyV4Service(
        repo,
        gyms,
        org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymPackagePriceRepository.class),
        org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymPrimaryDisableV4Repository.class),
        org.mockito.Mockito.mock(PrimaryGymNotificationV4Service.class),
        org.mockito.Mockito.mock(az.fitnest.catalog.client.OrderServiceGrpcClient.class));
  }

  @Test
  void entryTimeDeterminesPriceAndBronzeIsDeniedAfterFive() throws Exception {
    var repo =
        org.mockito.Mockito.mock(
            az.fitnest.catalog.repository.GymMembershipPolicyV4Repository.class);
    var gyms = org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymRepository.class);
    var packagePrices =
        org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymPackagePriceRepository.class);
    var disables =
        org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymPrimaryDisableV4Repository.class);
    var notify = org.mockito.Mockito.mock(PrimaryGymNotificationV4Service.class);
    var plans = org.mockito.Mockito.mock(az.fitnest.catalog.client.OrderServiceGrpcClient.class);
    var memberPackages =
        List.of(
            new PackageRule(
                1L,
                Tier.BRONZE,
                true,
                true,
                new BigDecimal("4.00"),
                new BigDecimal("50.00"),
                List.of(),
                null),
            new PackageRule(
                2L,
                Tier.SILVER,
                true,
                true,
                new BigDecimal("6.00"),
                new BigDecimal("60.00"),
                List.of(),
                null),
            new PackageRule(3L, Tier.GOLD, false, false, BigDecimal.ZERO, null, List.of(), null),
            new PackageRule(
                4L, Tier.PLATINUM, false, false, BigDecimal.ZERO, null, List.of(), null));
    var policy =
        new GymMembershipPolicyV4(
            0,
            true,
            memberPackages,
            List.of(
                rule("00:01", "16:59", Tier.BRONZE, Set.of(Tier.values())),
                rule(
                    "17:00", "23:59", Tier.SILVER, Set.of(Tier.SILVER, Tier.GOLD, Tier.PLATINUM))));
    var row = new GymMembershipPolicyV4Entity();
    row.setGymId(10L);
    row.setPayload(
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .writeValueAsString(policy));
    var gym =
        Gym.builder()
            .name("Test Gym")
            .status(az.fitnest.catalog.model.enums.GymStatus.ACTIVE)
            .generalWorkHours(
                Set.of(
                    new GymWorkHour(
                        GymWorkHourPeriod.MONDAY, LocalTime.of(0, 1), LocalTime.of(23, 59))))
            .build();
    org.mockito.Mockito.when(repo.findById(10L)).thenReturn(Optional.of(row));
    org.mockito.Mockito.when(gyms.findById(10L)).thenReturn(Optional.of(gym));
    var service = new GymMembershipPolicyV4Service(repo, gyms, packagePrices, disables, notify, plans);
    var before = LocalDateTime.of(2026, 10, 26, 16, 59, 59);
    var after = before.withHour(17).withMinute(0);
    assertTrue(service.quote(10L, 1L, before).allowed());
    assertFalse(service.quote(10L, 1L, after).allowed());
    assertEquals(new BigDecimal("6.00"), service.quote(10L, 2L, after).entryPrice());
    assertEquals(new BigDecimal("4.00"), service.quote(10L, 3L, before).entryPrice());
    assertEquals(new BigDecimal("6.00"), service.quote(10L, 3L, after).entryPrice());
    assertEquals(1, service.quote(10L, 1L, before).accessWindows().size());
  }

  @Test
  void saveCreatesGymSubscriptionForIncludedPackageEvenWithoutPrimary() {
    var repo =
        org.mockito.Mockito.mock(
            az.fitnest.catalog.repository.GymMembershipPolicyV4Repository.class);
    var gyms = org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymRepository.class);
    var packagePrices =
        org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymPackagePriceRepository.class);
    var disables =
        org.mockito.Mockito.mock(az.fitnest.catalog.repository.GymPrimaryDisableV4Repository.class);
    var notify = org.mockito.Mockito.mock(PrimaryGymNotificationV4Service.class);
    var plans = org.mockito.Mockito.mock(az.fitnest.catalog.client.OrderServiceGrpcClient.class);

    // Silver included but not primary — previously skipped gym_subscriptions and caused
    // GYM_NOT_SUPPORTED for Silver members at QR entrance.
    var memberPackages =
        List.of(
            new PackageRule(1L, Tier.BRONZE, false, false, BigDecimal.ZERO, null, List.of(), null),
            new PackageRule(
                2L,
                Tier.SILVER,
                true,
                false,
                new BigDecimal("7.00"),
                null,
                List.of("Fitness"),
                null),
            new PackageRule(
                3L,
                Tier.GOLD,
                true,
                false,
                new BigDecimal("12.00"),
                null,
                List.of("Fitness"),
                null),
            new PackageRule(
                4L, Tier.PLATINUM, false, false, BigDecimal.ZERO, null, List.of(), null));
    var policy = new GymMembershipPolicyV4(0, false, memberPackages, List.of());
    var gym =
        Gym.builder()
            .name("Premium Park Hotel")
            .status(az.fitnest.catalog.model.enums.GymStatus.ACTIVE)
            .subscriptions(new HashSet<>())
            .build();
    org.mockito.Mockito.when(gyms.findById(244L)).thenReturn(Optional.of(gym));
    org.mockito.Mockito.when(gyms.save(org.mockito.ArgumentMatchers.any())).thenAnswer(a -> a.getArgument(0));
    org.mockito.Mockito.when(repo.findById(244L)).thenReturn(Optional.empty());
    org.mockito.Mockito.when(repo.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            a -> {
              GymMembershipPolicyV4Entity e = a.getArgument(0);
              e.setVersion(0);
              return e;
            });
    org.mockito.Mockito.when(
            packagePrices.findByGymIdAndPackageIdAndEffectiveFrom(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.empty());
    org.mockito.Mockito.when(packagePrices.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(a -> a.getArgument(0));
    org.mockito.Mockito.when(plans.checkPackageExists(org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(true);

    var service = new GymMembershipPolicyV4Service(repo, gyms, packagePrices, disables, notify, plans);
    service.save(244L, new az.fitnest.catalog.dto.v4.GymPolicyUpdateV4(policy, false, Map.of()));

    Set<Long> synced =
        gym.getSubscriptions().stream().map(GymSubscription::getPackageId).collect(java.util.stream.Collectors.toSet());
    assertEquals(Set.of(2L, 3L), synced);
    assertEquals(
        7.0,
        gym.getSubscriptions().stream()
            .filter(s -> s.getPackageId().equals(2L))
            .findFirst()
            .orElseThrow()
            .getDailyPrice());
  }
}
