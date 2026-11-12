package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.response.GymAccessHoursResponse;
import az.fitnest.catalog.exception.UnauthorizedException;
import az.fitnest.catalog.service.GymAccessHoursService;
import az.fitnest.catalog.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing gym access hours (work hours + package windows + forUser).
 * Gateway route: {@code GET /api/v1/me/gyms/{gymId}/access-hours}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GymAccessHoursController {

    private final GymAccessHoursService gymAccessHoursService;

    @GetMapping("/v1/me/gyms/{gymId}/access-hours")
    @Operation(summary = "Get gym access hours for the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<GymAccessHoursResponse> getAccessHours(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long gymId) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new UnauthorizedException("error.unauthorized");
        }
        return ResponseEntity.ok(gymAccessHoursService.getAccessHours(gymId, userId));
    }
}
