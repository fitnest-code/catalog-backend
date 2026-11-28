package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.dashboard.DashboardGymObligationsResponse;
import az.fitnest.catalog.dto.dashboard.DashboardTopGymsResponse;
import az.fitnest.catalog.service.dashboard.DashboardGymService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Parallel dashboard gym APIs. Leaves {@link GymReportController} unchanged.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Gyms", description = "Admin dashboard gym obligations / top gyms")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'OWNER')")
public class DashboardGymController {

    private final DashboardGymService dashboardGymService;

    @Operation(summary = "Gym obligations for dashboard")
    @GetMapping("/gym-obligations")
    public ResponseEntity<DashboardGymObligationsResponse> getObligations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(dashboardGymService.getObligations(dateFrom, dateTo));
    }

    @Operation(summary = "Top gyms by obligation amount")
    @GetMapping("/top-gyms")
    public ResponseEntity<DashboardTopGymsResponse> getTopGyms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardGymService.getTopGyms(dateFrom, dateTo, limit));
    }
}
