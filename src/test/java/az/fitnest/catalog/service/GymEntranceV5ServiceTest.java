package az.fitnest.catalog.service;

import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_ID;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_MAIN;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_MAIN_2;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_SUB;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.category;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.gym;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import az.fitnest.catalog.dto.response.GymEntranceEligibilityResponse;
import az.fitnest.catalog.dto.v5.GymEntranceEligibilityRequestV5;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.category.CategoryEntryPriceResolver;
import az.fitnest.catalog.service.category.GymCategoryOfferingsService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class GymEntranceV5ServiceTest {

    private GymEntranceService classic;
    private GymRepository gymRepository;
    private GymCategoryOfferingsService offerings;
    private GymEntranceV5Service service;

    @BeforeEach
    void setUp() {
        classic = mock(GymEntranceService.class);
        gymRepository = mock(GymRepository.class);
        offerings = mock(GymCategoryOfferingsService.class);
        // Real resolver: validateCategory only touches the static owned-category checks, no repos needed.
        CategoryEntryPriceResolver resolver = new CategoryEntryPriceResolver(null, null);
        service = new GymEntranceV5Service(classic, gymRepository, offerings, resolver);
    }

    @Test
    void eligibilityReturnsChipsAndRequiresSelectionWhenSubsExist() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
        when(classic.checkGymEntranceEligibility(org.mockito.ArgumentMatchers.any()))
                .thenReturn(GymEntranceEligibilityResponse.builder().allowed(true).build());
        when(gymRepository.findWithDetailsById(GYM_ID)).thenReturn(Optional.of(gym));
        when(offerings.localizedCategoryName(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> ((az.fitnest.catalog.model.entity.Category) inv.getArgument(0)).getName());

        var response = service.checkEligibility(1L, new GymEntranceEligibilityRequestV5(GYM_ID, null));

        assertTrue(response.eligible());
        assertTrue(response.categorySelectionRequired());
        assertEquals(2, response.categories().size());
        assertEquals(GYM_MAIN, response.categories().get(0).categoryId());
        assertEquals("MAIN", response.categories().get(0).role());
        assertEquals(GYM_SUB, response.categories().get(1).categoryId());
        assertEquals("SUB", response.categories().get(1).role());
    }

    @Test
    void validateCategoryThrowsWhenRequiredAndMissing() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.validateCategory(gym, null));
        assertEquals("CATEGORY_ID_REQUIRED", ex.getReason());
    }

    @Test
    void validateCategoryThrowsForTwoMainsWithoutSelection() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness"), category(GYM_MAIN_2, "Yoga")), List.of(), false);

        assertThrows(ResponseStatusException.class, () -> service.validateCategory(gym, null));
    }

    @Test
    void validateCategoryAllowsMissingCategoryOnMainOnlyGym() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(), false);

        assertDoesNotThrow(() -> service.validateCategory(gym, null));
        assertFalse(GymCategoryOfferingsService.categorySelectionRequired(gym));
    }

    @Test
    void validateCategoryRejectsCategoryNotOnGym() {
        Gym gym = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.validateCategory(gym, 999L));
        assertEquals("CATEGORY_NOT_ON_GYM", ex.getReason());
    }
}
