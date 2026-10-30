package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.request.HomeGymPackagePriceRequest;
import az.fitnest.catalog.dto.request.HomeGymTimeSlotRequest;
import az.fitnest.catalog.dto.request.HomeGymToggleRequest;
import az.fitnest.catalog.dto.response.HomeGymAdminStateResponse;
import az.fitnest.catalog.dto.response.HomeGymPackagePriceResponse;
import az.fitnest.catalog.dto.response.HomeGymTimeSlotRuleResponse;
import az.fitnest.catalog.service.homegym.HomeGymAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin panel endpoints for home-gym configuration (spec 6.1-6.3, R14/R15/R17/R18).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Home Gym Admin", description = "Əsas zal açarı, paket qiymətləri və saat qaydalarının idarə edilməsi.")
@SecurityRequirement(name = "bearerAuth")
public class HomeGymAdminController {

    private final HomeGymAdminService homeGymAdminService;

    @Operation(summary = "Zalın əsas zal konfiqurasiyasını alın")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v1/admin/gyms/{gymId}/home-gym")
    public ResponseEntity<HomeGymAdminStateResponse> getState(@PathVariable Long gymId) {
        return ResponseEntity.ok(homeGymAdminService.getState(gymId));
    }

    @Operation(summary = "“Əsas zal kimi seçilə bilər” açarını dəyişin")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PutMapping("/v1/admin/gyms/{gymId}/home-gym")
    public ResponseEntity<HomeGymAdminStateResponse> setHomeEligible(
            @PathVariable Long gymId,
            @RequestBody HomeGymToggleRequest request) {
        return ResponseEntity.ok(homeGymAdminService.setHomeEligible(gymId, request));
    }

    @Operation(summary = "Zalın əsas zal paket qiymətlərini alın")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v1/admin/gyms/{gymId}/home-gym/prices")
    public ResponseEntity<List<HomeGymPackagePriceResponse>> getPrices(@PathVariable Long gymId) {
        return ResponseEntity.ok(homeGymAdminService.getPrices(gymId));
    }

    @Operation(summary = "Paket üçün əsas zal qiymətini yaradın və ya yeniləyin")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PutMapping("/v1/admin/gyms/{gymId}/home-gym/prices")
    public ResponseEntity<HomeGymPackagePriceResponse> upsertPrice(
            @PathVariable Long gymId,
            @RequestBody HomeGymPackagePriceRequest request) {
        return ResponseEntity.ok(homeGymAdminService.upsertPrice(gymId, request));
    }

    @Operation(summary = "Zalın əsas zal saat qaydalarını alın")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v1/admin/gyms/{gymId}/home-gym/time-slots")
    public ResponseEntity<List<HomeGymTimeSlotRuleResponse>> getTimeSlots(@PathVariable Long gymId) {
        return ResponseEntity.ok(homeGymAdminService.getTimeSlots(gymId));
    }

    @Operation(summary = "Yeni əsas zal saat qaydası yaradın")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PostMapping("/v1/admin/gyms/{gymId}/home-gym/time-slots")
    public ResponseEntity<HomeGymTimeSlotRuleResponse> createTimeSlot(
            @PathVariable Long gymId,
            @RequestBody HomeGymTimeSlotRequest request) {
        return ResponseEntity.ok(homeGymAdminService.createTimeSlot(gymId, request));
    }

    @Operation(summary = "Əsas zal saat qaydasını yeniləyin")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PutMapping("/v1/admin/gyms/{gymId}/home-gym/time-slots/{ruleId}")
    public ResponseEntity<HomeGymTimeSlotRuleResponse> updateTimeSlot(
            @PathVariable Long gymId,
            @PathVariable Long ruleId,
            @RequestBody HomeGymTimeSlotRequest request) {
        return ResponseEntity.ok(homeGymAdminService.updateTimeSlot(gymId, ruleId, request));
    }

    @Operation(summary = "Əsas zal saat qaydasını silin")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @DeleteMapping("/v1/admin/gyms/{gymId}/home-gym/time-slots/{ruleId}")
    public ResponseEntity<Void> deleteTimeSlot(
            @PathVariable Long gymId,
            @PathVariable Long ruleId) {
        homeGymAdminService.deleteTimeSlot(gymId, ruleId);
        return ResponseEntity.noContent().build();
    }
}
