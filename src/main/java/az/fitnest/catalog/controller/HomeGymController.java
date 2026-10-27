package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.HomeGymGymResponse;
import az.fitnest.catalog.exception.UnauthorizedException;
import az.fitnest.catalog.service.homegym.HomeGymCatalogService;
import az.fitnest.catalog.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing home-gym list served by catalog (spec screen 1.3/4.1).
 * The gateway must route this path before the broader order home-gym routes.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeGymController {

    private final HomeGymCatalogService homeGymCatalogService;

    @GetMapping("/v1/subscriptions/{subscriptionId}/home-gym/gyms")
    @Operation(summary = "List gyms available as home gym for a subscription")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaginatedResponse<HomeGymGymResponse>> getHomeGymGyms(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long subscriptionId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng) {
        Long userId = UserContext.extractUserId(principal);
        if (userId == null) {
            throw new UnauthorizedException("error.unauthorized");
        }
        return ResponseEntity.ok(homeGymCatalogService.getGyms(
                userId, subscriptionId, q, page, pageSize, lat, lng));
    }
}
