package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.v5.GymCategoryAnalyticsResponseV2;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.ServiceItem;
import az.fitnest.catalog.dto.v5.GymCategoryOfferingV5.WorkHourItem;
import az.fitnest.catalog.dto.v5.GymCategoryPriceRequestV5;
import az.fitnest.catalog.dto.v5.GymCategoryPriceResponseV5;
import az.fitnest.catalog.dto.v5.GymCategoryServicesRequestV5;
import az.fitnest.catalog.dto.v5.GymCategoryWorkHoursRequestV5;
import az.fitnest.catalog.service.category.GymCategoryOfferingsAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin CRUD for per-category (main/sub) prices, work hours and services, plus by-category analytics. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Gym Category Offerings Admin")
@SecurityRequirement(name = "bearerAuth")
public class GymCategoryOfferingsAdminController {

    private final GymCategoryOfferingsAdminService service;

    @Operation(summary = "Category package prices")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v1/admin/gyms/{gymId}/categories/{categoryId}/prices")
    public ResponseEntity<List<GymCategoryPriceResponseV5>> getPrices(
            @PathVariable Long gymId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(service.getPrices(gymId, categoryId));
    }

    @Operation(summary = "Create/update (versioned by effectiveFrom) a category package price")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PutMapping("/v1/admin/gyms/{gymId}/categories/{categoryId}/prices")
    public ResponseEntity<GymCategoryPriceResponseV5> upsertPrice(
            @PathVariable Long gymId, @PathVariable Long categoryId,
            @RequestBody GymCategoryPriceRequestV5 request) {
        return ResponseEntity.ok(service.upsertPrice(gymId, categoryId, request));
    }

    @Operation(summary = "Category work hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v1/admin/gyms/{gymId}/categories/{categoryId}/work-hours")
    public ResponseEntity<List<WorkHourItem>> getWorkHours(
            @PathVariable Long gymId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(service.getWorkHours(gymId, categoryId));
    }

    @Operation(summary = "Replace category work hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PutMapping("/v1/admin/gyms/{gymId}/categories/{categoryId}/work-hours")
    public ResponseEntity<List<WorkHourItem>> replaceWorkHours(
            @PathVariable Long gymId, @PathVariable Long categoryId,
            @RequestBody GymCategoryWorkHoursRequestV5 request) {
        return ResponseEntity.ok(service.replaceWorkHours(gymId, categoryId, request));
    }

    @Operation(summary = "Category services")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v1/admin/gyms/{gymId}/categories/{categoryId}/services")
    public ResponseEntity<List<ServiceItem>> getServices(
            @PathVariable Long gymId, @PathVariable Long categoryId) {
        return ResponseEntity.ok(service.getServices(gymId, categoryId));
    }

    @Operation(summary = "Replace category services")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @PutMapping("/v1/admin/gyms/{gymId}/categories/{categoryId}/services")
    public ResponseEntity<List<ServiceItem>> replaceServices(
            @PathVariable Long gymId, @PathVariable Long categoryId,
            @RequestBody GymCategoryServicesRequestV5 request) {
        return ResponseEntity.ok(service.replaceServices(gymId, categoryId, request));
    }

    @Operation(summary = "Entrance analytics grouped by category (null category = legacy)")
    @PreAuthorize("hasAnyRole('ADMIN', 'GYM_SUPER_ADMIN', 'GYM_ADMIN')")
    @GetMapping("/v2/admin/gyms/{id}/analytics/by-category")
    public ResponseEntity<GymCategoryAnalyticsResponseV2> analyticsByCategory(
            @PathVariable("id") Long gymId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(service.analyticsByCategory(gymId, startDate, endDate));
    }
}
