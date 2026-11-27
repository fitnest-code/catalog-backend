package az.fitnest.catalog.service.category;

import static az.fitnest.catalog.service.category.CategoryTestFixtures.BRONZE;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_ID;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_MAIN;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_MAIN_2;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_SUB;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.category;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.gym;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import az.fitnest.catalog.model.entity.GymCategoryServiceLink;
import az.fitnest.catalog.model.entity.GymCategoryWorkHour;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymCategoryServiceLinkRepository;
import az.fitnest.catalog.repository.GymCategoryWorkHourRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GymCategoryOfferingsServiceTest {

    private static final LocalDate EPOCH = LocalDate.of(2026, 1, 1);

    private GymPackagePriceRepository gymPrices;
    private GymCategoryPackagePriceRepository categoryPrices;
    private GymCategoryWorkHourRepository categoryWorkHours;
    private GymCategoryServiceLinkRepository categoryServices;
    private SupportedServiceRepository supportedServices;
    private HomeGymRuleService homeGymRuleService;
    private TranslationService translationService;
    private GymCategoryOfferingsService service;

    @BeforeEach
    void setUp() {
        gymPrices = mock(GymPackagePriceRepository.class);
        categoryPrices = mock(GymCategoryPackagePriceRepository.class);
        categoryWorkHours = mock(GymCategoryWorkHourRepository.class);
        categoryServices = mock(GymCategoryServiceLinkRepository.class);
        supportedServices = mock(SupportedServiceRepository.class);
        homeGymRuleService = mock(HomeGymRuleService.class);
        translationService = mock(TranslationService.class);
        service = new GymCategoryOfferingsService(gymPrices, categoryPrices, categoryWorkHours, categoryServices,
                supportedServices, homeGymRuleService, translationService);

        // Defaults = "nothing configured"; individual tests override what they exercise.
        when(categoryWorkHours.findByGymIdAndCategoryIdIn(any(), anyCollection())).thenReturn(List.of());
        when(categoryServices.findByGymIdAndCategoryIdIn(any(), anyCollection())).thenReturn(List.of());
        when(supportedServices.findAllById(any())).thenReturn(List.of());
        when(categoryPrices.findLatestByGymAndCategories(any(), anyCollection(), any())).thenReturn(List.of());
        when(gymPrices.findLatestByGymIdIn(any(), any())).thenReturn(List.of());
        when(homeGymRuleService.packageContext(BRONZE))
                .thenReturn(new HomeGymRuleService.PackageContext(BRONZE, "Bronze", 1, List.of(BRONZE)));
    }

    // ------------------------------------------------------------------
    // categorySelectionRequired
    // ------------------------------------------------------------------

    @Test
    void selectionNotRequiredForMainOnlyGym() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(), false);

        assertFalse(GymCategoryOfferingsService.categorySelectionRequired(gym));
    }

    @Test
    void selectionRequiredForMainPlusSub() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);

        assertTrue(GymCategoryOfferingsService.categorySelectionRequired(gym));
    }

    @Test
    void selectionRequiredForTwoMainsEvenWithoutSubs() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness"), category(GYM_MAIN_2, "Yoga")), List.of(), false);

        assertTrue(GymCategoryOfferingsService.categorySelectionRequired(gym));
    }

    // ------------------------------------------------------------------
    // buildOfferings
    // ------------------------------------------------------------------

    @Test
    void mainUsesGymPackagePriceAndSubUsesCategoryPackagePrice() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
        when(gymPrices.findLatestByGymIdIn(eq(List.of(GYM_ID)), any())).thenReturn(List.of(
                GymPackagePrice.builder().packageId(BRONZE).included(true)
                        .singlePrice(10.0).unlimitedPrice(80.0).effectiveFrom(EPOCH).build()));
        when(categoryPrices.findLatestByGymAndCategories(eq(GYM_ID), anyCollection(), any())).thenReturn(List.of(
                GymCategoryPackagePrice.builder().gym(gym).categoryId(GYM_SUB).packageId(BRONZE)
                        .singlePrice(18.0).unlimitedPrice(140.0).effectiveFrom(EPOCH).build()));

        List<GymCategoryOfferingV5> result = service.buildOfferings(gym, null);

        assertEquals(2, result.size());
        GymCategoryOfferingV5 main = result.get(0);
        GymCategoryOfferingV5 sub = result.get(1);
        assertEquals("MAIN", main.role());
        assertEquals(GYM_MAIN, main.categoryId());
        assertEquals(10.0, main.packagePrices().get(0).singlePrice());
        assertEquals(80.0, main.packagePrices().get(0).monthlyPrice());
        assertEquals("Bronze", main.packagePrices().get(0).packageName());

        assertEquals("SUB", sub.role());
        assertEquals(18.0, sub.packagePrices().get(0).singlePrice());
        assertEquals(140.0, sub.packagePrices().get(0).monthlyPrice());
    }

    @Test
    void subWithoutPriceRowGetsEmptyPriceListInsteadOfMainPrices() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
        when(gymPrices.findLatestByGymIdIn(any(), any())).thenReturn(List.of(
                GymPackagePrice.builder().packageId(BRONZE).included(true)
                        .singlePrice(10.0).unlimitedPrice(80.0).effectiveFrom(EPOCH).build()));

        List<GymCategoryOfferingV5> result = service.buildOfferings(gym, null);

        assertEquals(1, result.get(0).packagePrices().size());
        assertTrue(result.get(1).packagePrices().isEmpty());
    }

    @Test
    void workHoursFallBackToGymGeneralHoursWhenCategoryHoursEmpty() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
        gym.setGeneralWorkHours(Set.of(
                new GymWorkHour(GymWorkHourPeriod.MONDAY, LocalTime.of(8, 0), LocalTime.of(22, 0))));
        when(categoryWorkHours.findByGymIdAndCategoryIdIn(eq(GYM_ID), anyCollection())).thenReturn(List.of(
                GymCategoryWorkHour.builder().gymId(GYM_ID).categoryId(GYM_SUB)
                        .period(GymWorkHourPeriod.TUESDAY)
                        .fromTime(LocalTime.of(10, 0)).toTime(LocalTime.of(20, 0)).build()));

        List<GymCategoryOfferingV5> result = service.buildOfferings(gym, null);

        // Main has no category rows -> general hours; sub has its own row -> general hours ignored.
        var mainHours = result.get(0).workHours();
        assertEquals(1, mainHours.size());
        assertEquals("MONDAY", mainHours.get(0).period());
        assertEquals("08:00", mainHours.get(0).from());

        var subHours = result.get(1).workHours();
        assertEquals(1, subHours.size());
        assertEquals("TUESDAY", subHours.get(0).period());
        assertEquals("10:00", subHours.get(0).from());
        assertEquals("20:00", subHours.get(0).to());
    }

    @Test
    void servicesComeFromCategoryLinksWhenPresent() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
        SupportedService sauna = SupportedService.builder().name("Sauna").iconUrl("sauna.png").build();
        sauna.setId(7001L);
        when(categoryServices.findByGymIdAndCategoryIdIn(eq(GYM_ID), anyCollection())).thenReturn(List.of(
                GymCategoryServiceLink.builder().gymId(GYM_ID).categoryId(GYM_SUB).supportedServiceId(7001L).build()));
        when(supportedServices.findAllById(any())).thenReturn(List.of(sauna));

        List<GymCategoryOfferingV5> result = service.buildOfferings(gym, null);

        assertTrue(result.get(0).services().isEmpty(), "main has no links and no legacy subscriptions");
        var subServices = result.get(1).services();
        assertEquals(1, subServices.size());
        assertEquals(7001L, subServices.get(0).id());
        assertEquals("Sauna", subServices.get(0).name());
    }

    @Test
    void localizedNameFallsBackToCategoryNameWhenNoTranslation() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(), false);
        when(translationService.getTranslatedValue("CATEGORY", GYM_MAIN.toString(), "name", "RU"))
                .thenReturn("Фитнес");

        assertEquals("Фитнес", service.buildOfferings(gym, "RU").get(0).name());
        assertEquals("Fitness", service.buildOfferings(gym, "EN").get(0).name());
    }
}
