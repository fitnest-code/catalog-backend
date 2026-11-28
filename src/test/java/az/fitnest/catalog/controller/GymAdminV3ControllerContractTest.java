package az.fitnest.catalog.controller;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the V3 controller contract (paths, HTTP verbs, multipart, admin role, bean validation)
 * and checks that the existing V1/V2 gym wizard mappings are still present unchanged.
 */
class GymAdminV3ControllerContractTest {

    @Test
    void classLevelApiPrefixRemainsUnchanged() {
        RequestMapping mapping = GymAdminController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api"}, mapping.value());
    }

    @Test
    void allV3WizardEndpointsKeepTheirPathsVerbsAndAdminRole() {
        assertEndpoint("createGymCompleteV3", PostMapping.class, "/v3/admin/gyms/create-complete", true);
        assertEndpoint("createGymStep1V3", PostMapping.class, "/v3/admin/gyms/step1", false);
        assertEndpoint("validateStep1V3", PostMapping.class, "/v3/admin/gyms/validate/step1", false);
        assertEndpoint("createGymStep6V3", PostMapping.class, "/v3/admin/gyms/{id}/step6", true);
        assertEndpoint("validateStep6V3", PostMapping.class, "/v3/admin/gyms/validate/step6", true);
        assertEndpoint("updateGymSubscriptionsV3", PutMapping.class, "/v3/admin/gyms/{id}/subscriptions", false);
    }

    @Test
    void everyV3PayloadIsBeanValidated() {
        for (String name : new String[]{"createGymCompleteV3", "createGymStep1V3", "validateStep1V3",
                "createGymStep6V3", "validateStep6V3", "updateGymSubscriptionsV3"}) {
            Method method = method(name);
            boolean validated = Arrays.stream(method.getParameterAnnotations())
                    .anyMatch(annotations -> Arrays.stream(annotations)
                            .anyMatch(a -> a.annotationType() == Valid.class));
            assertTrue(validated, () -> name + " must validate its request payload");
        }
    }

    @Test
    void legacyV2WizardMappingsAreStillRegistered() {
        assertEndpoint("createGymCompleteV2", PostMapping.class, "/v2/admin/gyms/create-complete", true);
        assertEndpoint("createGymStep1V2", PostMapping.class, "/v2/admin/gyms/step1", false);
        assertEndpoint("validateStep1V2", PostMapping.class, "/v2/admin/gyms/validate/step1", false);
        assertEndpoint("createGymStep5V2", PostMapping.class, "/v2/admin/gyms/{id}/step5", true);
        assertEndpoint("validateStep5V2", PostMapping.class, "/v2/admin/gyms/validate/step5", true);
        assertEndpoint("createGymStep6V2", PostMapping.class, "/v2/admin/gyms/{id}/step6", true);
        assertEndpoint("validateStep6V2", PostMapping.class, "/v2/admin/gyms/validate/step6", true);
        assertEndpoint("updateGymSubscriptionsV2", PutMapping.class, "/v2/admin/gyms/{id}/subscriptions", false);
    }

    private static void assertEndpoint(String methodName, Class<? extends Annotation> mappingType,
                                       String path, boolean multipart) {
        Method method = method(methodName);
        Annotation mapping = method.getAnnotation(mappingType);
        assertNotNull(mapping, () -> methodName + " must carry " + mappingType.getSimpleName());
        try {
            assertArrayEquals(new String[]{path}, (String[]) mappingType.getMethod("value").invoke(mapping));
            if (multipart) {
                assertArrayEquals(new String[]{MediaType.MULTIPART_FORM_DATA_VALUE},
                        (String[]) mappingType.getMethod("consumes").invoke(mapping));
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, () -> methodName + " must be admin-only");
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    private static Method method(String name) {
        return Arrays.stream(GymAdminController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing controller method: " + name));
    }
}
