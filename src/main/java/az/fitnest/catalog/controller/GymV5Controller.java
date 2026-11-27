package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.v5.GymDetailResponseV5;
import az.fitnest.catalog.dto.v5.GymEntranceEligibilityRequestV5;
import az.fitnest.catalog.dto.v5.GymEntranceEligibilityResponseV5;
import az.fitnest.catalog.dto.v5.GymEntranceScanRequestV5;
import az.fitnest.catalog.dto.v5.GymEntranceScanResponseV5;
import az.fitnest.catalog.service.GymDetailV5Service;
import az.fitnest.catalog.service.GymEntranceV4Service;
import az.fitnest.catalog.service.GymEntranceV5Service;
import az.fitnest.catalog.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v5 gym endpoints: category-aware (main + sub) detail and QR entrance. Parallel to v1/v4 on purpose so
 * those response contracts stay frozen for released mobile builds.
 */
@RestController
@RequestMapping("/api/v5/gyms")
@RequiredArgsConstructor
@Tag(name = "Gyms V5", description = "Kateqoriya (əsas + alt) dəstəkli zal təfərrüatı və giriş")
@SecurityRequirement(name = "bearerAuth")
public class GymV5Controller {

    private final GymDetailV5Service detailService;
    private final GymEntranceV5Service entranceService;
    private final GymEntranceV4Service primaryEntranceService;

    @GetMapping("/{gymId:\\d+}")
    @Operation(summary = "Gym detail v5 (v4 detail + category offerings)")
    public ResponseEntity<GymDetailResponseV5> getGymDetail(
            @AuthenticationPrincipal Object principal, @PathVariable Long gymId) {
        return ResponseEntity.ok(detailService.getGymDetail(UserContext.extractUserId(principal), gymId));
    }

    @PostMapping("/entrance/eligibility")
    @Operation(summary = "Entrance eligibility v5 (adds category chips)",
            description = "Optional body {gymId | qrCodeValue} yields the category picker chips for that gym.")
    public ResponseEntity<GymEntranceEligibilityResponseV5> eligibility(
            @AuthenticationPrincipal Object principal,
            @RequestBody(required = false) GymEntranceEligibilityRequestV5 request) {
        return ResponseEntity.ok(entranceService.checkEligibility(principal, request));
    }

    @PostMapping("/entrance/scan")
    @Operation(summary = "QR entrance scan v5 (category-aware)")
    public ResponseEntity<GymEntranceScanResponseV5> scan(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody GymEntranceScanRequestV5 request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        try {
            var response = entranceService.scan(principal, request, userAgent);
            return ResponseEntity.status(statusFor(response)).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        }
    }

    /** Mirrors v4 primary-gym scan; categoryId is mandatory when the gym has subs / several categories. */
    @PostMapping("/entrance/primary-scan")
    @Operation(summary = "Primary Gym check-in v5 (adds optional categoryId)")
    public Object primaryScan(@Valid @RequestBody GymEntranceV4Service.Scan input) {
        return primaryEntranceService.scan(input, true);
    }

    // Same decision -> HTTP status mapping as the classic v1 scan endpoint in GymController.
    private static int statusFor(GymEntranceScanResponseV5 response) {
        var result = response.result();
        String decision = result == null ? null : result.decision();
        if ("CONFIRMATION_REQUIRED".equals(decision) || "QR_SCAN_CONFLICT".equals(decision)) return 409;
        if ("INVALID_QR_SCAN_ID".equals(decision)) return 400;
        if ("OTHER_GYMS_LOCKED".equals(decision)
                || "VISIT_LIMIT_EXCEEDED".equals(decision)
                || "DAILY_LIMIT_REACHED".equals(decision)
                || "HOME_MONTHLY_LIMIT_REACHED".equals(decision)
                || "TIER_NOT_ALLOWED_AT_THIS_TIME".equals(decision)
                || "SUBSCRIPTION_FROZEN".equals(decision)) return 403;
        return 200;
    }
}
