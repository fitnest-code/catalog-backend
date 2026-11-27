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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Main categories (and legacy null categoryId) must keep reading gym_package_price; only subcategories use
 * gym_category_package_price. Error codes are part of the v5 client contract, so they are asserted verbatim.
 */
class CategoryEntryPriceResolverTest {

    private static final LocalDate EPOCH = LocalDate.of(2026, 1, 1);

    private GymCategoryPackagePriceRepository categoryPrices;
    private GymPackagePriceRepository gymPrices;
    private CategoryEntryPriceResolver resolver;

    @BeforeEach
    void setUp() {
        categoryPrices = mock(GymCategoryPackagePriceRepository.class);
        gymPrices = mock(GymPackagePriceRepository.class);
        resolver = new CategoryEntryPriceResolver(categoryPrices, gymPrices);
    }

    @Test
    void mainOnlyGymWithNullCategoryUsesGymPackagePrice() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(), false);
        stubGymPrice(12.0, 90.0, true);

        var prices = resolver.resolve(gym, BRONZE, null);

        assertFalse(prices.fromSubcategoryTable());
        assertEquals(12.0, prices.singlePrice());
        assertEquals(90.0, prices.monthlyPrice());
        // null categoryId falls back to the first main category so history can still be bucketed.
        assertEquals(GYM_MAIN, prices.categoryId());
        verifyNoInteractions(categoryPrices);
    }

    @Test
    void subcategoryWithConfiguredPriceUsesSubcategoryTable() {
        Gym gym = mixedGym();
        GymCategoryPackagePrice row = GymCategoryPackagePrice.builder()
                .gym(gym).categoryId(GYM_SUB).packageId(BRONZE)
                .singlePrice(18.5).unlimitedPrice(140.0).effectiveFrom(EPOCH).build();
        when(categoryPrices
                        .findFirstByGymIdAndCategoryIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                eq(GYM_ID), eq(GYM_SUB), eq(BRONZE), any(LocalDate.class)))
                .thenReturn(Optional.of(row));

        var prices = resolver.resolve(gym, BRONZE, GYM_SUB);

        assertTrue(prices.fromSubcategoryTable());
        assertEquals(18.5, prices.singlePrice());
        assertEquals(140.0, prices.monthlyPrice());
        assertEquals(GYM_SUB, prices.categoryId());
        verifyNoInteractions(gymPrices);
    }

    @Test
    void subcategoryWithoutPriceRowFailsInsteadOfFallingBackToMainPrice() {
        Gym gym = mixedGym();
        when(categoryPrices
                        .findFirstByGymIdAndCategoryIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        assertReason("CATEGORY_PRICE_NOT_CONFIGURED", () -> resolver.resolve(gym, BRONZE, GYM_SUB));
    }

    @Test
    void categoryNotOnGymIsRejected() {
        Gym gym = mixedGym();

        assertReason("CATEGORY_NOT_ON_GYM", () -> resolver.resolve(gym, BRONZE, 999L));
    }

    @Test
    void gymWithSubcategoriesRequiresCategoryId() {
        assertReason("CATEGORY_ID_REQUIRED", () -> resolver.resolve(mixedGym(), BRONZE, null));
    }

    @Test
    void hasSubcategoriesFlagAloneAlsoRequiresCategoryId() {
        // Flag set but sub rows not linked (yet): still must not silently charge the main price.
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(), true);

        assertReason("CATEGORY_ID_REQUIRED", () -> resolver.resolve(gym, BRONZE, null));
    }

    @Test
    void mainCategoryOnMixedGymUsesGymPackagePriceNotSubcategoryTable() {
        Gym gym = mixedGym();
        stubGymPrice(10.0, 80.0, true);

        var prices = resolver.resolve(gym, BRONZE, GYM_MAIN);

        assertFalse(prices.fromSubcategoryTable());
        assertEquals(10.0, prices.singlePrice());
        assertEquals(GYM_MAIN, prices.categoryId());
        verifyNoInteractions(categoryPrices);
    }

    @Test
    void dualMainGymAcceptsEitherMainWithGymPackagePrice() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness"), category(GYM_MAIN_2, "Yoga")), List.of(), false);
        stubGymPrice(9.0, 70.0, true);

        var prices = resolver.resolve(gym, BRONZE, GYM_MAIN_2);

        assertFalse(prices.fromSubcategoryTable());
        assertEquals(GYM_MAIN_2, prices.categoryId());
    }

    @Test
    void missingGymPriceRowYieldsZeroPrices() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(), false);
        when(gymPrices.findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        any(), any(), any()))
                .thenReturn(Optional.empty());

        var prices = resolver.resolve(gym, BRONZE, null);

        assertEquals(0.0, prices.singlePrice());
        assertEquals(0.0, prices.monthlyPrice());
    }

    private static Gym mixedGym() {
        return gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
    }

    private void stubGymPrice(Double single, Double unlimited, boolean included) {
        GymPackagePrice price = GymPackagePrice.builder()
                .packageId(BRONZE).included(included).singlePrice(single).unlimitedPrice(unlimited)
                .effectiveFrom(EPOCH).build();
        when(gymPrices.findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        eq(GYM_ID), eq(BRONZE), any(LocalDate.class)))
                .thenReturn(Optional.of(price));
    }

    private static void assertReason(String expected, org.junit.jupiter.api.function.Executable call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call);
        assertEquals(expected, ex.getReason());
    }
}
