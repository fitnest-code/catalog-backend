package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.LandingGymCardV2Response;
import az.fitnest.catalog.dto.response.LandingGymDetailV2Response;
import az.fitnest.catalog.dto.response.LandingGymFiltersResponse;
import az.fitnest.catalog.service.LandingPublicV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v2/public/landing")
@RequiredArgsConstructor
@Tag(name = "Landing Public v2", description = "Unauthenticated landing gym endpoints (v2). Min-entry tier, no phone.")
public class LandingPublicV2Controller {

    // Same policy as v1 JSON so edge/CDN behavior is identical across versions.
    private static final CacheControl JSON_CACHE = CacheControl
            .maxAge(Duration.ofMinutes(2))
            .cachePublic()
            .staleWhileRevalidate(Duration.ofMinutes(2));

    private final LandingPublicV2Service landingPublicV2Service;

    @Operation(summary = "Gym listing filters", description = "Distinct cities, categories, and memberships for active gyms.")
    @GetMapping("/gyms/filters")
    public ResponseEntity<LandingGymFiltersResponse> getGymFilters() {
        return cachedJson(landingPublicV2Service.getGymFilters());
    }

    @Operation(summary = "Gym listing cards (v2)", description = "Paginated cards with minEntryMembership. page_size is capped.")
    @GetMapping("/gyms")
    public ResponseEntity<PaginatedResponse<LandingGymCardV2Response>> getGyms(
            @Parameter(description = "Page index, starting at 1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page, max 50") @RequestParam(value = "page_size", defaultValue = "10") int pageSize,
            @Parameter(description = "Search name, city, or address") @RequestParam(required = false) String q,
            @Parameter(description = "Exact city name") @RequestParam(required = false) String city,
            @Parameter(description = "Bakı rayon name") @RequestParam(required = false) String rayon,
            @Parameter(description = "Exact category name") @RequestParam(required = false) String category,
            @Parameter(description = "Exact min entry tier: bronze, silver, gold, or platinum") @RequestParam(required = false) String membership) {
        return cachedJson(landingPublicV2Service.getGyms(
                Math.min(Math.max(page, 1), 40),
                Math.min(Math.max(pageSize, 1), 50),
                q,
                city,
                rayon,
                category,
                membership));
    }

    @Operation(summary = "Gym detail (v2)", description = "Public gym profile with minEntryMembership. No phone, email, QR, or staff.")
    @GetMapping("/gyms/{gymId:\\d+}")
    public ResponseEntity<LandingGymDetailV2Response> getGym(@PathVariable Long gymId) {
        return cachedJson(landingPublicV2Service.getGym(gymId));
    }

    private <T> ResponseEntity<T> cachedJson(T body) {
        return ResponseEntity.ok()
                .cacheControl(JSON_CACHE)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.VARY, "Accept-Language")
                .body(body);
    }
}
