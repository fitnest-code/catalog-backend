package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.dto.request.HomeGymPackagePriceRequest;
import az.fitnest.catalog.dto.request.HomeGymTimeSlotRequest;
import az.fitnest.catalog.dto.request.HomeGymToggleRequest;
import az.fitnest.catalog.dto.response.HomeGymAdminStateResponse;
import az.fitnest.catalog.dto.response.HomeGymPackagePriceResponse;
import az.fitnest.catalog.dto.response.HomeGymTimeSlotRuleResponse;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ForbiddenException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.exception.UnprocessableEntityException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.GymAdminRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Admin home-gym management (spec mock 6.1-6.3): R18 master switch, versioned prices (R15/R17)
 * and time-slot validation/versioning (R14/F-03).
 */
@SuppressWarnings("unchecked")
class HomeGymAdminServiceTest {

    private static final Long GYM_ID = 100L;
    private static final Long ADMIN_ID = 9L;
    private static final Long BRONZE = 1201L;
    private static final Long SILVER = 1202L;
    private static final Long GOLD = 1203L;
    private static final Long PLATINUM = 1204L;
    private static final LocalDate TODAY = LocalDate.now(HomeGymRuleService.BAKU);
    private static final LocalDate NEXT_MONTH = TODAY.withDayOfMonth(1).plusMonths(1);

    private GymRepository gymRepository;
    private GymAdminRepository gymAdminRepository;
    private GymPackagePriceRepository packagePriceRepository;
    private GymTimeSlotRuleRepository timeSlotRuleRepository;
    private HomeGymRuleService ruleService;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private HomeGymAdminService service;
    private Gym gym;

    @BeforeEach
    void setUp() {
        gymRepository = mock(GymRepository.class);
        gymAdminRepository = mock(GymAdminRepository.class);
        packagePriceRepository = mock(GymPackagePriceRepository.class);
        timeSlotRuleRepository = mock(GymTimeSlotRuleRepository.class);
        ruleService = mock(HomeGymRuleService.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        service = new HomeGymAdminService(gymRepository, gymAdminRepository, packagePriceRepository,
                timeSlotRuleRepository, ruleService, kafkaTemplate);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ADMIN_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        gym = gym();
        when(gymRepository.findWithDetailsById(GYM_ID)).thenReturn(Optional.of(gym));
        when(packagePriceRepository.findByGymIdOrderByEffectiveFromDescPackageIdAsc(GYM_ID))
                .thenReturn(List.of());
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of());
        when(timeSlotRuleRepository.save(any(GymTimeSlotRule.class))).thenAnswer(invocation -> {
            GymTimeSlotRule rule = invocation.getArgument(0);
            if (rule.getId() == null) {
                rule.setId(80L);
            }
            return rule;
        });
        when(packagePriceRepository.save(any(GymPackagePrice.class))).thenAnswer(invocation -> {
            GymPackagePrice price = invocation.getArgument(0);
            if (price.getId() == null) {
                price.setId(40L);
            }
            return price;
        });
        when(ruleService.packageContext(any())).thenAnswer(invocation -> context(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // R18 master switch
    // ------------------------------------------------------------------

    @Test
    void theSwitchRequiresTheEnabledFlag() {
        assertEquals("HOME_GYM_INVALID_REQUEST",
                assertThrows(BadRequestException.class, () -> service.setHomeEligible(GYM_ID, null))
                        .getErrorCode());
        assertEquals("HOME_GYM_INVALID_REQUEST",
                assertThrows(BadRequestException.class,
                        () -> service.setHomeEligible(GYM_ID, new HomeGymToggleRequest()))
                        .getErrorCode());
        verify(gymRepository, never()).save(any(Gym.class));
    }

    @Test
    void switchingOffPublishesTheLocalizedR18Event() {
        HomeGymToggleRequest request = HomeGymToggleRequest.builder()
                .enabled(false)
                .title(Map.of("az", "Zal baglandi", "en", "Gym disabled", "ru", "Zal otkluchen"))
                .body(Map.of("az", "Yeni esas zal secin", "en", "Pick another gym", "ru", "Vyberite drugoy zal"))
                .recipients(java.util.Arrays.asList(9L, 9L, null))
                .build();

        HomeGymAdminStateResponse state = service.setHomeEligible(GYM_ID, request);

        assertFalse(gym.getHomeEligible());
        assertFalse(state.homeEligible());
        verify(gymRepository).save(gym);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(HomeGymAdminService.HOME_GYM_EVENTS_TOPIC), eq("100"), captor.capture());
        Map<String, Object> event = (Map<String, Object>) captor.getValue();
        assertEquals(HomeGymAdminService.HOME_GYM_DISABLED, event.get("eventType"));
        assertEquals(GYM_ID, event.get("gymId"));
        assertEquals("Zal baglandi", ((Map<String, String>) event.get("title")).get("AZ"));
        assertEquals("Yeni esas zal secin", ((Map<String, String>) event.get("body")).get("AZ"));
        assertEquals(List.of(9L), event.get("recipients"));
        assertEquals(ADMIN_ID, event.get("createdBy"));
    }

    @Test
    void switchingOffRequiresEverySupportedTranslation() {
        HomeGymToggleRequest request = HomeGymToggleRequest.builder()
                .enabled(false)
                .title(Map.of("az", "Basliq", "en", "Title"))
                .body(Map.of("az", "Metn", "en", "Body", "ru", "Tekst"))
                .build();

        assertEquals("HOME_GYM_MISSING_TRANSLATION",
                assertThrows(BadRequestException.class, () -> service.setHomeEligible(GYM_ID, request))
                        .getErrorCode());
        // The gym row is written before the translations are validated; @Transactional rolls
        // it back in production, and no R18 event may leave the service.
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void anOverlongTitleIsRejected() {
        HomeGymToggleRequest request = HomeGymToggleRequest.builder()
                .enabled(false)
                .title(Map.of("az", "a".repeat(66), "en", "Title", "ru", "Zagolovok"))
                .body(Map.of("az", "Metn", "en", "Body", "ru", "Tekst"))
                .build();

        assertEquals("HOME_GYM_TEXT_TOO_LONG",
                assertThrows(BadRequestException.class, () -> service.setHomeEligible(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void switchingOnDoesNotPublishAnything() {
        gym.setHomeEligible(false);

        HomeGymAdminStateResponse state = service.setHomeEligible(GYM_ID,
                HomeGymToggleRequest.builder().enabled(true).build());

        assertTrue(gym.getHomeEligible());
        assertTrue(state.homeEligible());
        verify(gymRepository).save(gym);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void anUnchangedSwitchIsANoOp() {
        service.setHomeEligible(GYM_ID, HomeGymToggleRequest.builder().enabled(true).build());

        verify(gymRepository, never()).save(any(Gym.class));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void stateListsPricesAndTimeSlots() {
        GymPackagePrice price = priceRow(GOLD);
        GymTimeSlotRule rule = slot(5L, "MONDAY,TUESDAY", LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(packagePriceRepository.findByGymIdOrderByEffectiveFromDescPackageIdAsc(GYM_ID))
                .thenReturn(List.of(price));
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of(rule));

        HomeGymAdminStateResponse state = service.getState(GYM_ID);

        assertEquals(GYM_ID, state.gymId());
        assertEquals("FitZone", state.gymName());
        assertTrue(state.homeEligible());
        assertEquals(1, state.prices().size());
        assertEquals("Gold", state.prices().get(0).packageName());
        assertEquals(1, state.timeSlots().size());
        HomeGymTimeSlotRuleResponse slot = state.timeSlots().get(0);
        assertEquals(List.of("MONDAY", "TUESDAY"), slot.weekdays());
        assertEquals("09:00", slot.startTime());
        assertEquals("18:00", slot.endTime());
        assertEquals(List.of(GOLD), slot.allowedPackageIds());
        assertEquals("Gold", slot.pricePackageName());
    }

    // ------------------------------------------------------------------
    // Versioned prices (R15/R17)
    // ------------------------------------------------------------------

    @Test
    void aPriceRequestMustNameAPackage() {
        assertEquals("HOME_GYM_INVALID_REQUEST",
                assertThrows(BadRequestException.class, () -> service.upsertPrice(GYM_ID, null))
                        .getErrorCode());
        assertEquals("HOME_GYM_INVALID_REQUEST",
                assertThrows(BadRequestException.class,
                        () -> service.upsertPrice(GYM_ID, HomeGymPackagePriceRequest.builder().build()))
                        .getErrorCode());
    }

    @Test
    void aPriceForAPackageTheGymDoesNotOfferIsRejected() {
        HomeGymPackagePriceRequest request = priceRequest(PLATINUM, 10.0, 100.0);

        assertEquals("HOME_GYM_PACKAGE_NOT_IN_GYM",
                assertThrows(BadRequestException.class, () -> service.upsertPrice(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void negativePricesAreRejected() {
        assertEquals("HOME_GYM_INVALID_PRICE",
                assertThrows(BadRequestException.class,
                        () -> service.upsertPrice(GYM_ID, priceRequest(GOLD, -1.0, 100.0)))
                        .getErrorCode());
        assertEquals("HOME_GYM_INVALID_PRICE",
                assertThrows(BadRequestException.class,
                        () -> service.upsertPrice(GYM_ID, priceRequest(GOLD, 10.0, -100.0)))
                        .getErrorCode());
    }

    @Test
    void aHomeEligiblePriceNeedsTheUnlimitedPrice() {
        HomeGymPackagePriceRequest request = HomeGymPackagePriceRequest.builder()
                .packageId(GOLD)
                .homeEligible(true)
                .singlePrice(10.0)
                .build();

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.upsertPrice(GYM_ID, request));

        // A missing unlimited price fails the non-negative check before the home-eligible check.
        assertEquals("HOME_GYM_INVALID_PRICE", error.getErrorCode());
        verify(packagePriceRepository, never()).save(any(GymPackagePrice.class));
    }

    @Test
    void aNewPriceIsWrittenForTheFirstOfNextMonth() {
        when(packagePriceRepository.findByGymIdAndPackageIdAndEffectiveFrom(GYM_ID, GOLD, NEXT_MONTH))
                .thenReturn(Optional.empty());

        HomeGymPackagePriceResponse response =
                service.upsertPrice(GYM_ID, priceRequest(GOLD, 10.0, 100.0));

        assertEquals(40L, response.id());
        assertEquals(GOLD, response.packageId());
        assertEquals("Gold", response.packageName());
        assertTrue(response.included());
        assertEquals(10.0, response.singlePrice());
        assertEquals(100.0, response.unlimitedPrice());
        assertEquals(NEXT_MONTH, response.effectiveFrom());

        ArgumentCaptor<GymPackagePrice> captor = ArgumentCaptor.forClass(GymPackagePrice.class);
        verify(packagePriceRepository).save(captor.capture());
        assertSame(gym, captor.getValue().getGym());
    }

    @Test
    void aHistoricEffectiveMonthIsBumpedToNextMonth() {
        LocalDate requested = NEXT_MONTH.minusMonths(2).withDayOfMonth(20);
        when(packagePriceRepository.findByGymIdAndPackageIdAndEffectiveFrom(GYM_ID, GOLD, NEXT_MONTH))
                .thenReturn(Optional.empty());

        HomeGymPackagePriceResponse response = service.upsertPrice(GYM_ID,
                HomeGymPackagePriceRequest.builder()
                        .packageId(GOLD).singlePrice(10.0).unlimitedPrice(100.0)
                        .effectiveFrom(requested).build());

        assertEquals(NEXT_MONTH, response.effectiveFrom());
    }

    @Test
    void aFutureEffectiveMonthIsNormalizedToTheFirstDay() {
        LocalDate future = NEXT_MONTH.plusMonths(2).withDayOfMonth(17);
        when(packagePriceRepository.findByGymIdAndPackageIdAndEffectiveFrom(
                GYM_ID, GOLD, future.withDayOfMonth(1))).thenReturn(Optional.empty());

        HomeGymPackagePriceResponse response = service.upsertPrice(GYM_ID,
                HomeGymPackagePriceRequest.builder()
                        .packageId(GOLD).singlePrice(10.0).unlimitedPrice(100.0)
                        .effectiveFrom(future).build());

        assertEquals(future.withDayOfMonth(1), response.effectiveFrom());
    }

    @Test
    void anExistingRowForTheSameMonthIsUpdatedInPlace() {
        GymPackagePrice existing = priceRow(GOLD);
        existing.setId(41L);
        existing.setSinglePrice(5.0);
        when(packagePriceRepository.findByGymIdAndPackageIdAndEffectiveFrom(GYM_ID, GOLD, NEXT_MONTH))
                .thenReturn(Optional.of(existing));

        HomeGymPackagePriceResponse response = service.upsertPrice(GYM_ID,
                HomeGymPackagePriceRequest.builder()
                        .packageId(GOLD).singlePrice(12.0).unlimitedPrice(120.0)
                        .included(false).build());

        assertEquals(41L, response.id());
        assertEquals(12.0, existing.getSinglePrice());
        assertEquals(120.0, existing.getUnlimitedPrice());
        assertFalse(existing.getIncluded());
        verify(packagePriceRepository).save(existing);
    }

    // ------------------------------------------------------------------
    // Time slots: validation (R14/F-03)
    // ------------------------------------------------------------------

    @Test
    void aTimeSlotRequestIsRequired() {
        assertEquals("HOME_GYM_INVALID_REQUEST",
                assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, null))
                        .getErrorCode());
    }

    @Test
    void invalidTimeWindowsAreRejected() {
        assertEquals("HOME_GYM_INVALID_TIME_WINDOW", errorOf(timeSlot("18:00", "09:00")));
        assertEquals("HOME_GYM_INVALID_TIME_WINDOW", errorOf(timeSlot("09:00", null)));
        assertEquals("HOME_GYM_INVALID_TIME_WINDOW", errorOf(timeSlot("25:00", "26:00")));
    }

    @Test
    void anUnknownWeekdayIsRejected() {
        HomeGymTimeSlotRequest request = timeSlot("09:00", "18:00");
        request.setWeekdays(List.of("FUNDAY"));

        assertEquals("HOME_GYM_INVALID_WEEKDAY",
                assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void allowedPackagesAreRequired() {
        HomeGymTimeSlotRequest request = timeSlot("09:00", "18:00");
        request.setAllowedPackageIds(List.of());

        assertEquals("HOME_GYM_ALLOWED_PACKAGES_REQUIRED",
                assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void thePricePackageIsRequired() {
        HomeGymTimeSlotRequest request = timeSlot("09:00", "18:00");
        request.setPricePackageId(null);

        assertEquals("HOME_GYM_PRICE_PACKAGE_REQUIRED",
                assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void aPackageOutsideTheGymHierarchyIsRejected() {
        HomeGymTimeSlotRequest request = timeSlot("09:00", "18:00");
        request.setAllowedPackageIds(List.of(9999L));

        assertEquals("HOME_GYM_PACKAGE_NOT_IN_GYM",
                assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void aWindowOutsideTheGymWorkHoursIsRejected() {
        assertEquals("HOME_GYM_OUTSIDE_WORK_HOURS", errorOf(timeSlot("08:00", "10:00")));
        assertEquals("HOME_GYM_OUTSIDE_WORK_HOURS", errorOf(timeSlot("17:00", "19:00")));
    }

    @Test
    void aWeekdayWithoutWorkHoursIsRejected() {
        HomeGymTimeSlotRequest request = timeSlot("09:00", "18:00");
        request.setWeekdays(List.of("WEDNESDAY"));

        assertEquals("HOME_GYM_OUTSIDE_WORK_HOURS",
                assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, request))
                        .getErrorCode());
    }

    @Test
    void anOverlappingWindowIsRejected() {
        GymTimeSlotRule existing = slot(5L, "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0));
        existing.setEffectiveTo(null);
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of(existing));

        HomeGymTimeSlotRequest request = timeSlot("11:00", "13:00");

        assertEquals("TIME_SLOT_OVERLAP",
                assertThrows(UnprocessableEntityException.class,
                        () -> service.createTimeSlot(GYM_ID, request)).getErrorCode());
    }

    @Test
    void adjacentMinuteWindowsDoNotOverlap() {
        GymTimeSlotRule existing = slot(5L, "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0));
        existing.setEffectiveTo(null);
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of(existing));

        HomeGymTimeSlotRuleResponse response = service.createTimeSlot(GYM_ID, timeSlot("12:01", "13:00"));

        assertEquals("12:01", response.startTime());
        assertEquals("13:00", response.endTime());
    }

    @Test
    void nonIntersectingWeekdaysDoNotOverlap() {
        GymTimeSlotRule existing = slot(5L, "MONDAY", LocalTime.of(10, 0), LocalTime.of(12, 0));
        existing.setEffectiveTo(null);
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of(existing));
        HomeGymTimeSlotRequest request = timeSlot("10:00", "12:00");
        request.setWeekdays(List.of("TUESDAY"));

        HomeGymTimeSlotRuleResponse response = service.createTimeSlot(GYM_ID, request);

        assertEquals(List.of("TUESDAY"), response.weekdays());
    }

    @Test
    void aLegacyAllDayRuleBlocksEveryWindow() {
        GymTimeSlotRule legacy = slot(5L, null, null, null);
        legacy.setEffectiveTo(null);
        when(timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(GYM_ID))
                .thenReturn(List.of(legacy));

        assertEquals("TIME_SLOT_OVERLAP",
                assertThrows(UnprocessableEntityException.class,
                        () -> service.createTimeSlot(GYM_ID, timeSlot("09:00", "10:00"))).getErrorCode());
    }

    @Test
    void aPricePackageExcludedFromTheOfferIsUnprocessable() {
        GymPackagePrice excluded = priceRow(GOLD);
        excluded.setIncluded(false);
        when(packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(GOLD), any(LocalDate.class)))
                .thenReturn(Optional.of(excluded));

        assertEquals("HOME_GYM_PRICE_PACKAGE_NOT_INCLUDED",
                assertThrows(UnprocessableEntityException.class,
                        () -> service.createTimeSlot(GYM_ID, timeSlot("09:00", "18:00"))).getErrorCode());
    }

    // ------------------------------------------------------------------
    // Time slots: creation and versioning
    // ------------------------------------------------------------------

    @Test
    void creationNormalizesAndStoresTheRule() {
        HomeGymTimeSlotRequest request = timeSlot("09:00", "18:00");
        request.setWeekdays(List.of("monday", " Monday "));

        HomeGymTimeSlotRuleResponse response = service.createTimeSlot(GYM_ID, request);

        assertEquals(80L, response.id());
        assertEquals(List.of("MONDAY"), response.weekdays());
        assertEquals("09:00", response.startTime());
        assertEquals("18:00", response.endTime());
        assertEquals(List.of(GOLD), response.allowedPackageIds());
        assertEquals(GOLD, response.pricePackageId());
        assertEquals("Gold", response.pricePackageName());
        assertEquals(NEXT_MONTH, response.effectiveFrom());
        assertNull(response.effectiveTo());

        ArgumentCaptor<GymTimeSlotRule> captor = ArgumentCaptor.forClass(GymTimeSlotRule.class);
        verify(timeSlotRuleRepository).save(captor.capture());
        GymTimeSlotRule saved = captor.getValue();
        assertEquals("MONDAY", saved.getWeekdays());
        assertEquals(Set.of(GOLD), new LinkedHashSet<>(saved.getAllowedPackageIds()));
        assertSame(gym, saved.getGym());
    }

    @Test
    void anEffectiveRuleIsClosedAndANewVersionIsCreated() {
        GymTimeSlotRule existing = slot(7L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        existing.setEffectiveFrom(NEXT_MONTH.minusMonths(1));
        existing.setEffectiveTo(null);
        when(timeSlotRuleRepository.findByIdAndGymId(7L, GYM_ID)).thenReturn(Optional.of(existing));

        HomeGymTimeSlotRuleResponse response =
                service.updateTimeSlot(GYM_ID, 7L, timeSlot("09:00", "18:00"));

        assertEquals(80L, response.id());
        assertEquals(NEXT_MONTH.minusDays(1), existing.getEffectiveTo());
        assertEquals(NEXT_MONTH, response.effectiveFrom());
        verify(timeSlotRuleRepository, times(2)).save(any(GymTimeSlotRule.class));
    }

    @Test
    void aFutureRuleIsEditedInPlace() {
        GymTimeSlotRule future = slot(7L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        future.setEffectiveFrom(NEXT_MONTH);
        when(timeSlotRuleRepository.findByIdAndGymId(7L, GYM_ID)).thenReturn(Optional.of(future));

        HomeGymTimeSlotRuleResponse response =
                service.updateTimeSlot(GYM_ID, 7L, timeSlot("09:00", "18:00"));

        assertEquals(7L, response.id());
        assertEquals("18:00", response.endTime());
        assertEquals(NEXT_MONTH, future.getEffectiveFrom());
        verify(timeSlotRuleRepository).save(future);
    }

    @Test
    void updatingAnUnknownRuleIsNotFound() {
        when(timeSlotRuleRepository.findByIdAndGymId(7L, GYM_ID)).thenReturn(Optional.empty());

        assertEquals("HOME_GYM_TIME_SLOT_NOT_FOUND",
                assertThrows(ResourceNotFoundException.class,
                        () -> service.updateTimeSlot(GYM_ID, 7L, timeSlot("09:00", "18:00")))
                        .getErrorCode());
    }

    @Test
    void deletingAnEffectiveRuleClosesItAtTheMonthEnd() {
        GymTimeSlotRule existing = slot(7L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        existing.setEffectiveFrom(NEXT_MONTH.minusMonths(1));
        existing.setEffectiveTo(null);
        when(timeSlotRuleRepository.findByIdAndGymId(7L, GYM_ID)).thenReturn(Optional.of(existing));

        service.deleteTimeSlot(GYM_ID, 7L);

        assertEquals(NEXT_MONTH.minusDays(1), existing.getEffectiveTo());
        verify(timeSlotRuleRepository).save(existing);
        verify(timeSlotRuleRepository, never()).delete(any(GymTimeSlotRule.class));
    }

    @Test
    void deletingAFutureRuleRemovesIt() {
        GymTimeSlotRule future = slot(7L, "MONDAY", LocalTime.of(9, 0), LocalTime.of(17, 0));
        future.setEffectiveFrom(NEXT_MONTH);
        when(timeSlotRuleRepository.findByIdAndGymId(7L, GYM_ID)).thenReturn(Optional.of(future));

        service.deleteTimeSlot(GYM_ID, 7L);

        verify(timeSlotRuleRepository).delete(future);
        verify(timeSlotRuleRepository, never()).save(any(GymTimeSlotRule.class));
    }

    @Test
    void deletingAnUnknownRuleIsNotFound() {
        when(timeSlotRuleRepository.findByIdAndGymId(7L, GYM_ID)).thenReturn(Optional.empty());

        assertEquals("HOME_GYM_TIME_SLOT_NOT_FOUND",
                assertThrows(ResourceNotFoundException.class, () -> service.deleteTimeSlot(GYM_ID, 7L))
                        .getErrorCode());
    }

    // ------------------------------------------------------------------
    // Access control and not-found
    // ------------------------------------------------------------------

    @Test
    void aMissingGymIsNotFound() {
        when(gymRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertEquals("GYM_NOT_FOUND",
                assertThrows(ResourceNotFoundException.class, () -> service.getState(999L)).getErrorCode());
        assertEquals("GYM_NOT_FOUND",
                assertThrows(ResourceNotFoundException.class, () -> service.getState(null)).getErrorCode());
    }

    @Test
    void aGymAdminWithoutTheAdminRoleNeedsAnExplicitGrant() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                ADMIN_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(gymAdminRepository.existsByGymIdAndUserId(GYM_ID, ADMIN_ID)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.getState(GYM_ID));

        when(gymAdminRepository.existsByGymIdAndUserId(GYM_ID, ADMIN_ID)).thenReturn(true);
        assertEquals(GYM_ID, service.getState(GYM_ID).gymId());
    }

    @Test
    void anAnonymousRequestIsForbidden() {
        SecurityContextHolder.clearContext();

        assertThrows(ForbiddenException.class, () -> service.getState(GYM_ID));
        verify(gymAdminRepository, never()).existsByGymIdAndUserId(anyLong(), anyLong());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String errorOf(HomeGymTimeSlotRequest request) {
        return assertThrows(BadRequestException.class, () -> service.createTimeSlot(GYM_ID, request))
                .getErrorCode();
    }

    private static HomeGymTimeSlotRequest timeSlot(String start, String end) {
        return HomeGymTimeSlotRequest.builder()
                .weekdays(List.of("MONDAY"))
                .startTime(start)
                .endTime(end)
                .allowedPackageIds(List.of(GOLD))
                .pricePackageId(GOLD)
                .build();
    }

    private static HomeGymPackagePriceRequest priceRequest(Long packageId, Double single, Double unlimited) {
        return HomeGymPackagePriceRequest.builder()
                .packageId(packageId)
                .singlePrice(single)
                .unlimitedPrice(unlimited)
                .build();
    }

    private static HomeGymRuleService.PackageContext context(Long packageId) {
        if (packageId == null) {
            return new HomeGymRuleService.PackageContext(null, null, 0, List.of());
        }
        String name;
        int rank;
        if (packageId.equals(BRONZE)) {
            name = "Bronze";
            rank = 1;
        } else if (packageId.equals(SILVER)) {
            name = "Silver";
            rank = 2;
        } else if (packageId.equals(GOLD)) {
            name = "Gold";
            rank = 3;
        } else if (packageId.equals(PLATINUM)) {
            name = "Platinum";
            rank = 4;
        } else {
            return new HomeGymRuleService.PackageContext(packageId, null, 0, List.of(packageId));
        }
        return new HomeGymRuleService.PackageContext(packageId, name, rank,
                List.of(BRONZE, SILVER, GOLD, PLATINUM).subList(0, rank));
    }

    private static Gym gym() {
        GymSubscription subscription = new GymSubscription();
        subscription.setPackageId(GOLD);
        GymWorkHour monday = GymWorkHour.builder()
                .period(GymWorkHourPeriod.MONDAY)
                .fromTime(LocalTime.of(9, 0))
                .toTime(LocalTime.of(18, 0))
                .build();
        GymWorkHour tuesday = GymWorkHour.builder()
                .period(GymWorkHourPeriod.TUESDAY)
                .fromTime(LocalTime.of(9, 0))
                .toTime(LocalTime.of(18, 0))
                .build();
        Gym gym = Gym.builder()
                .name("FitZone")
                .homeEligible(true)
                .subscriptions(new LinkedHashSet<>(Set.of(subscription)))
                .generalWorkHours(new LinkedHashSet<>(Set.of(monday, tuesday)))
                .build();
        gym.setId(GYM_ID);
        return gym;
    }

    private static GymPackagePrice priceRow(Long packageId) {
        return GymPackagePrice.builder()
                .gym(null)
                .packageId(packageId)
                .included(true)
                .homeEligible(true)
                .singlePrice(10.0)
                .unlimitedPrice(100.0)
                .effectiveFrom(NEXT_MONTH)
                .build();
    }

    private static GymTimeSlotRule slot(Long id, String weekdays, LocalTime start, LocalTime end) {
        GymTimeSlotRule rule = new GymTimeSlotRule();
        rule.setId(id);
        rule.setWeekdays(weekdays);
        rule.setStartTime(start);
        rule.setEndTime(end);
        rule.setAllowedPackageIds(new LinkedHashSet<>(Set.of(GOLD)));
        rule.setPricePackageId(GOLD);
        rule.setEffectiveFrom(NEXT_MONTH.minusMonths(1));
        return rule;
    }
}
