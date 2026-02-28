    /*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  io.swagger.v3.oas.annotations.Parameter
 *  io.swagger.v3.oas.annotations.media.Content
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.responses.ApiResponse
 *  io.swagger.v3.oas.annotations.responses.ApiResponses
 *  io.swagger.v3.oas.annotations.security.SecurityRequirement
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  jakarta.validation.Valid
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.access.prepost.PreAuthorize
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.GymDetailResponse;
import az.fitnest.catalog.dto.GymImageDto;
import az.fitnest.catalog.dto.LocationDto;
import az.fitnest.catalog.dto.GymQrResponse;
import az.fitnest.catalog.dto.GymImageResponse;
import az.fitnest.catalog.dto.GymMainPageDto;
import az.fitnest.catalog.dto.PaginatedResponse;

import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymSubscriptionsUpdateRequest;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.ReviewRequest;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.dto.UpdateImageUrlRequest;
import az.fitnest.catalog.service.impl.GymImageService;
import az.fitnest.catalog.service.impl.GymReadService;
import az.fitnest.catalog.service.impl.GymReviewService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/gyms")
@RequiredArgsConstructor
@Tag(name = "Gyms", description = "Endpoints for managing and exploring gyms, including packages, trainers, and user reviews")
public class GymController {
    private final GymReadService gymReadService;
    private final GymWriteService gymWriteService;
    private final GymImageService gymImageService;
    private final GymReviewService gymReviewService;
    private final GymTrainerService gymTrainerService;

    @GetMapping("/{gymId:\\d+}")
    @Operation(summary="Get gym details", description="Retrieves full details of a specific gym, including location, facilities, and user-specific favorite status.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Gym details retrieved successfully", content={@Content(schema=@Schema(implementation=GymDetailResponse.class))}), @ApiResponse(responseCode="404", description="Gym not found")})
    public ResponseEntity<GymDetailResponse> getGymDetail(@AuthenticationPrincipal Object principal, @Parameter(description="ID of the gym") @PathVariable Long gymId) {
        Long userId = this.extractUserId(principal);
        return ResponseEntity.ok(this.gymReadService.getGymDetail(userId, gymId));
    }

    @GetMapping("/{gymId}/images")
    @Operation(summary="Get gym images", description="Returns a list of all images associated with the gym (logo, cover, interior).")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Images retrieved successfully", content={@Content(schema=@Schema(implementation=GymImageResponse.class))})})
    public ResponseEntity<GymImageResponse> getGymImages(@PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymReadService.getGymImages(gymId));
    }

    @GetMapping("/{gymId}/qr")
    @Operation(summary="Get gym QR code URL", description="Returns a stream URL for the gym's QR code image.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="QR code URL retrieved successfully", content={@Content(schema=@Schema(implementation=GymQrResponse.class))})})
    public ResponseEntity<GymQrResponse> getGymQrUrl(@Parameter(description="ID of the gym") @PathVariable Long gymId) {
        String qrCodeUrl = this.gymReadService.getGymDetail(null, gymId).getQr_code_url();
        return ResponseEntity.ok(new GymQrResponse(qrCodeUrl));
    }



    @GetMapping("/{gymId}/trainers")
    @Operation(summary="Get gym trainers", description="Returns a paginated list of trainers working at the gym.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Trainers retrieved successfully")})
    public ResponseEntity<PaginatedResponse<GymTrainerDto>> getTrainers(@PathVariable Long gymId, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size) {
        return ResponseEntity.ok(this.gymTrainerService.getTrainers(gymId, page, page_size));
    }

    @GetMapping("/{gymId}/reviews")
    @Operation(summary="Get gym reviews", description="Returns a paginated list of user reviews for the gym.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Reviews retrieved successfully")})
    public ResponseEntity<PaginatedResponse<GymReviewDto>> getReviews(@PathVariable Long gymId, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size, @Parameter(description="Sort order (e.g., newest, highest_rating)") @RequestParam(required=false) String sort) {
        return ResponseEntity.ok(this.gymReviewService.getReviews(gymId, page, page_size, sort));
    }

    @PostMapping("/{gymId}/reviews")
    @Operation(summary="Submit a review", description="Allows an authenticated user to post a rating and comment for a gym.")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="201", description="Review submitted successfully"), @ApiResponse(responseCode="401", description="User not authenticated"), @ApiResponse(responseCode="400", description="Invalid rating scale (1-5)")})
    public ResponseEntity<Void> addReview(@AuthenticationPrincipal Object principal, @PathVariable Long gymId, @Valid @RequestBody ReviewRequest request) {
        Long userId = this.extractUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        this.gymReviewService.addReview(userId, gymId, request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{gymId}/check-in")
    @Operation(summary="Check-in to a gym", description="Allows an authenticated user to check-in using the gym's QR code. Deducts one entry from the active subscription.")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={
            @ApiResponse(responseCode="200", description="Check-in successful", content={@Content(schema=@Schema(implementation=az.fitnest.catalog.dto.CheckInResponseDto.class))}),
            @ApiResponse(responseCode="400", description="Check-in failed (e.g., no active subscription or expired)"),
            @ApiResponse(responseCode="401", description="User not authenticated")
    })
    public ResponseEntity<az.fitnest.catalog.dto.CheckInResponseDto> checkIn(@AuthenticationPrincipal Object principal, @PathVariable Long gymId) {
        Long userId = this.extractUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(this.gymWriteService.checkIn(userId, gymId));
        } catch (Exception e) {
            throw new az.fitnest.catalog.exception.BadRequestException("CHECKIN_FAILED", e.getMessage());
        }
    }

    @GetMapping("/{gymId}/reservation-rules")
    @Operation(summary="Get reservation rules", description="Returns specific rules for gym entry and reservation (e.g., time limits, cancellation policy).")
    public ResponseEntity<Object> getReservationRules(@PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymReadService.getReservationRules(gymId));
    }

    @GetMapping
    @Operation(summary="Get gyms", description="Consolidated endpoint for all gym listings (All, Closest, New, Saved). Use 'type' parameter to switch views.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Gyms retrieved successfully", content={@Content(schema=@Schema(implementation=PaginatedResponse.class))})})
    public ResponseEntity<PaginatedResponse<GymMainPageDto>> getGyms(
            @AuthenticationPrincipal Object principal,
            @Parameter(description="Search query") @RequestParam(value="q", required=false) String q,
            @Parameter(description="Filter type (ALL, NEW, CLOSEST, SAVED)") @RequestParam(value="type", defaultValue="ALL") String type,
            @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page,
            @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size,
            @Parameter(description="User latitude") @RequestParam(value="lat", required=false) Double lat,
            @Parameter(description="User longitude") @RequestParam(value="lng", required=false) Double lng) {
        Long userId = this.extractUserId(principal);
        return ResponseEntity.ok(this.gymReadService.getGyms(userId, q, type, page, page_size, lat, lng));
    }

    @PostMapping("/{gymId}/save")
    @Operation(summary = "Save/Unsave gym", description = "Toggles the 'saved' status of a gym for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Toggle successful"),
            @ApiResponse(responseCode = "404", description = "Gym not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<java.util.Map<String, Boolean>> toggleSave(@AuthenticationPrincipal Object principal, @PathVariable Long gymId) {
        Long userId = this.extractUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean isSaved = this.gymWriteService.toggleSave(userId, gymId);
        return ResponseEntity.ok(java.util.Map.of("is_saved", isSaved));
    }
    @GetMapping("/{gymId}/location")
    @Operation(summary = "Get gym location", description = "Returns the resolved address text along with latitude and longitude for the gym.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Location retrieved successfully", content = {@Content(schema = @Schema(implementation = LocationDto.class))}), @ApiResponse(responseCode = "404", description = "Gym not found")})
    public ResponseEntity<LocationDto> getGymLocation(@Parameter(description = "ID of the gym") @PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymReadService.getGymLocation(gymId));
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }
}

