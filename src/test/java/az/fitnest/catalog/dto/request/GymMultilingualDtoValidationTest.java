package az.fitnest.catalog.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bean Validation coverage for the new V3 gym wizard contracts. */
class GymMultilingualDtoValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static GymCreateStep1RequestV3 validStep1() {
        return GymCreateStep1RequestV3.builder()
                .mainCategoryDetails(List.of(new CategoryDetailV3(1L, null, "AZ", "EN", "RU", null)))
                .name("Gym")
                .phone("0501112233")
                .description("AZ about")
                .descriptionEn("EN about")
                .descriptionRu("RU about")
                .email("admin@fitnest.az")
                .hasSubcategories(false)
                .lessonTypeIds(List.of())
                .build();
    }

    private static GymCreateStep6SubscriptionRequestV3 validSubscription() {
        return new GymCreateStep6SubscriptionRequestV3(
                5L, 1L, 20.0, List.of(3L),
                List.of(new LocalizedServiceNameRequest("Massage", "Massage EN", "Массаж")));
    }

    @Test
    void validRequestsHaveNoViolations() {
        assertTrue(VALIDATOR.validate(validStep1()).isEmpty());
        assertTrue(VALIDATOR.validate(new GymCreateStep6RequestV3(List.of(validSubscription()))).isEmpty());
        assertTrue(VALIDATOR.validate(validComplete()).isEmpty());
    }

    @Test
    void step1RejectsMissingMainCategoryAndBlankNameAndInvalidPhone() {
        Set<String> fields = fields(VALIDATOR.validate(GymCreateStep1RequestV3.builder()
                .name("  ")
                .phone("123")
                .descriptionEn("x".repeat(2001))
                .email("not-an-email")
                .build()));

        assertTrue(fields.contains("mainCategoryDetails"));
        assertTrue(fields.contains("name"));
        assertTrue(fields.contains("phone"));
        assertTrue(fields.contains("descriptionEn"));
        assertTrue(fields.contains("email"));
    }

    @Test
    void step1ValidatesNestedCategoryDetails() {
        Set<String> fields = fields(VALIDATOR.validate(GymCreateStep1RequestV3.builder()
                .mainCategoryDetails(List.of(new CategoryDetailV3(null, null, "AZ", "x".repeat(2001), "RU", null)))
                .name("Gym")
                .phone("0501112233")
                .build()));

        assertTrue(fields.stream().anyMatch(f -> f.contains("categoryId")));
        assertTrue(fields.stream().anyMatch(f -> f.contains("descriptionEn")));
    }

    @Test
    void step6RejectsEmptySubscriptionsAndInvalidCustomServiceNames() {
        Set<String> emptyFields = fields(VALIDATOR.validate(new GymCreateStep6RequestV3(List.of())));
        assertTrue(emptyFields.contains("subscriptions"));

        Set<String> fields = fields(VALIDATOR.validate(new GymCreateStep6RequestV3(List.of(
                new GymCreateStep6SubscriptionRequestV3(null, 1L, 20.0, null,
                        List.of(new LocalizedServiceNameRequest(" ", "EN", "y".repeat(256))))))));

        assertTrue(fields.stream().anyMatch(f -> f.contains("packageId")));
        assertTrue(fields.stream().anyMatch(f -> f.contains("customServices")));
    }

    @Test
    void completeRequestRejectsMissingSubscriptionsAndAdmins() {
        Set<String> fields = fields(VALIDATOR.validate(GymCreateCompleteRequestV3.builder()
                .mainCategoryDetails(validStep1().mainCategoryDetails())
                .name("Gym")
                .phone("0501112233")
                .latitude(40.0)
                .longitude(49.0)
                .build()));

        assertTrue(fields.contains("subscriptions"));
        assertTrue(fields.contains("admins"));
        assertFalse(fields.contains("descriptionRu"), "absent optional translations are valid");
    }

    private static GymCreateCompleteRequestV3 validComplete() {
        return GymCreateCompleteRequestV3.builder()
                .mainCategoryDetails(validStep1().mainCategoryDetails())
                .name("Gym")
                .phone("0501112233")
                .description("AZ")
                .descriptionEn("EN")
                .descriptionRu("RU")
                .latitude(40.0)
                .longitude(49.0)
                .subscriptions(List.of(validSubscription()))
                .admins(List.of(new GymAdminCreateRequest(
                        "Admin", "User", "0501112233", "admin@fitnest.az", "pass123", "Super admin")))
                .build();
    }

    private static Set<String> fields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
