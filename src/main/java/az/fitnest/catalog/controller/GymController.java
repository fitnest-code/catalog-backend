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
import az.fitnest.catalog.dto.GymImageResponse;
import az.fitnest.catalog.dto.GymMainPageDto;
import az.fitnest.catalog.dto.GymPackageIncludesResponse;
import az.fitnest.catalog.dto.GymPackagesResponse;
import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymReviewsResponse;
import az.fitnest.catalog.dto.GymTrainersResponse;
import az.fitnest.catalog.dto.ReviewRequest;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.dto.UpdateImageUrlRequest;
import az.fitnest.catalog.service.GymService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping(value={"/api/v1"})
@Tag(name="Gyms", description="Endpoints for managing and exploring gyms, including packages, trainers, and user reviews")
public class GymController {
    private final GymService gymService;

    @GetMapping(value={"/gyms/{gymId}"})
    @Operation(summary="Get gym details", description="Retrieves full details of a specific gym, including location, facilities, and user-specific favorite status.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Gym details retrieved successfully", content={@Content(schema=@Schema(implementation=GymDetailResponse.class))}), @ApiResponse(responseCode="404", description="Gym not found")})
    public ResponseEntity<GymDetailResponse> getGymDetail(@AuthenticationPrincipal Object principal, @Parameter(description="ID of the gym") @PathVariable Long gymId) {
        Long userId = this.extractUserId(principal);
        return ResponseEntity.ok(this.gymService.getGymDetail(userId, gymId));
    }

    @GetMapping(value={"/gyms/{gymId}/images"})
    @Operation(summary="Get gym images", description="Returns a list of all images associated with the gym (logo, cover, interior).")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Images retrieved successfully", content={@Content(schema=@Schema(implementation=GymImageResponse.class))})})
    public ResponseEntity<GymImageResponse> getGymImages(@PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymService.getGymImages(gymId));
    }

    @GetMapping(value={"/gyms/{gymId}/packages"})
    @Operation(summary="Get gym packages", description="Returns all subscription packages offered by the gym.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Packages retrieved successfully", content={@Content(schema=@Schema(implementation=GymPackagesResponse.class))})})
    public ResponseEntity<GymPackagesResponse> getGymPackages(@PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymService.getGymPackages(gymId));
    }

    @GetMapping(value={"/gyms/{gymId}/packages/{packageId}/includes"})
    @Operation(summary="Get package inclusions", description="Returns what is included in a specific gym package (e.g., crossfit, sauna, pool).")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Inclusions retrieved successfully")})
    public ResponseEntity<GymPackageIncludesResponse> getPackageIncludes(@PathVariable Long gymId, @PathVariable Long packageId) {
        return ResponseEntity.ok(this.gymService.getPackageIncludes(gymId, packageId));
    }

    @GetMapping(value={"/gyms/{gymId}/trainers"})
    @Operation(summary="Get gym trainers", description="Returns a paginated list of trainers working at the gym.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Trainers retrieved successfully")})
    public ResponseEntity<GymTrainersResponse> getTrainers(@PathVariable Long gymId, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size) {
        return ResponseEntity.ok(this.gymService.getTrainers(gymId, page, page_size));
    }

    @GetMapping(value={"/gyms/{gymId}/reviews"})
    @Operation(summary="Get gym reviews", description="Returns a paginated list of user reviews for the gym.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Reviews retrieved successfully")})
    public ResponseEntity<GymReviewsResponse> getReviews(@PathVariable Long gymId, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size, @Parameter(description="Sort order (e.g., newest, highest_rating)") @RequestParam(required=false) String sort) {
        return ResponseEntity.ok(this.gymService.getReviews(gymId, page, page_size, sort));
    }

    @PostMapping(value={"/gyms/{gymId}/reviews"})
    @Operation(summary="Submit a review", description="Allows an authenticated user to post a rating and comment for a gym.")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="201", description="Review submitted successfully"), @ApiResponse(responseCode="401", description="User not authenticated"), @ApiResponse(responseCode="400", description="Invalid rating scale (1-5)")})
    public ResponseEntity<Void> addReview(@AuthenticationPrincipal Object principal, @PathVariable Long gymId, @Valid @RequestBody ReviewRequest request) {
        Long userId = this.extractUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        this.gymService.addReview(userId, gymId, request);
        return ResponseEntity.status(201).build();
    }

    @GetMapping(value={"/gyms/{gymId}/reservation-rules"})
    @Operation(summary="Get reservation rules", description="Returns specific rules for gym entry and reservation (e.g., time limits, cancellation policy).")
    public ResponseEntity<Object> getReservationRules(@PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymService.getReservationRules(gymId));
    }

    @GetMapping(value={"/gyms/closest"})
    @Operation(summary="Get closest gyms", description="Returns the list of gyms closest to the provided coordinates. Supports search and pagination. Returns address text (not latitude/longitude) and distance in kilometers.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Closest gyms retrieved", content={@Content(schema=@Schema(implementation=GymMainPageDto.class))})})
    public ResponseEntity<List<GymMainPageDto>> getClosestGyms(@Parameter(description="Search query") @RequestParam(value="q", required=false) String q, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size, @Parameter(description="User latitude") @RequestParam(value="lat", required=false) Double lat, @Parameter(description="User longitude") @RequestParam(value="lng", required=false) Double lng) {
        return ResponseEntity.ok(this.gymService.getClosestGyms(q, page, page_size, lat, lng));
    }

    @PutMapping(value={"/gyms/{gymId}/images/upload"}, consumes={"multipart/form-data"})
    @Operation(summary="Upload gym image", description="Uploads a new image for the gym. Requires partner or admin access.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Image uploaded successfully")})
    public ResponseEntity<GymImageDto> uploadGymImage(@PathVariable Long gymId, @Parameter(description="Descriptive name for the image") @RequestParam String imageName, @Parameter(description="Image file to upload") @RequestParam(value="file") MultipartFile file) {
        return ResponseEntity.ok(this.gymService.uploadGymImage(gymId, imageName, file));
    }

    @PostMapping(value={"/admin/gyms"})
    @Operation(summary="Create gym (Admin)", description="Creates a new gym profile. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="201", description="Gym created successfully"), @ApiResponse(responseCode="403", description="Insufficient permissions")})
    public ResponseEntity<Void> createGym(@RequestBody GymRequest request) {
        this.gymService.createGym(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping(value={"/admin/gyms/{gymId}"})
    @Operation(summary="Update gym (Admin)", description="Updates basic information of an existing gym. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="204", description="Gym updated successfully"), @ApiResponse(responseCode="404", description="Gym not found")})
    public ResponseEntity<Void> updateGym(@PathVariable Long gymId, @RequestBody GymRequest request) {
        this.gymService.updateGym(gymId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value={"/admin/gyms/{gymId}"})
    @Operation(summary="Delete gym (Admin)", description="Deletes a gym profile and all associated data. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="204", description="Gym deleted successfully")})
    public ResponseEntity<Void> deleteGym(@PathVariable Long gymId) {
        this.gymService.deleteGym(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value={"/admin/gyms/{gymId}/trainers"})
    @Operation(summary="Add trainer (Admin)", description="Registers a new trainer for a specific gym. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="201", description="Trainer added successfully")})
    public ResponseEntity<Void> addTrainer(@PathVariable Long gymId, @RequestBody TrainerRequest request) {
        this.gymService.addTrainer(gymId, request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping(value={"/admin/gyms/{gymId}/trainers/{trainerId}"})
    @Operation(summary="Update trainer (Admin)", description="Updates trainer information. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<Void> updateTrainer(@PathVariable Long gymId, @PathVariable Long trainerId, @RequestBody TrainerRequest request) {
        this.gymService.updateTrainer(gymId, trainerId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value={"/admin/gyms/{gymId}/trainers/{trainerId}"})
    @Operation(summary="Delete trainer (Admin)", description="Removes a trainer profile from a gym. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTrainer(@PathVariable Long gymId, @PathVariable Long trainerId) {
        this.gymService.deleteTrainer(gymId, trainerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value={"/admin/gyms/{gymId}/logo"})
    @Operation(summary="Update logo (Admin)", description="Updates the logo image URL for a gym. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<Void> updateGymLogo(@PathVariable Long gymId, @RequestBody UpdateImageUrlRequest request) {
        this.gymService.updateLogoUrl(gymId, request.getUrl());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value={"/admin/gyms/{gymId}/cover"})
    @Operation(summary="Update cover image (Admin)", description="Updates the cover image URL for a gym. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<Void> updateGymCover(@PathVariable Long gymId, @RequestBody UpdateImageUrlRequest request) {
        this.gymService.updateCoverImageUrl(gymId, request.getUrl());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value={"/admin/gyms/{gymId}/rooms/{imageName}/images"}, consumes={"multipart/form-data"})
    @Operation(summary="Upload room images (Admin)", description="Bulk uploads multiple interior images for a gym. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<List<GymImageDto>> uploadRoomImages(@PathVariable Long gymId, @PathVariable String imageName, @RequestParam(value="files") MultipartFile[] files) {
        return ResponseEntity.ok(this.gymService.uploadRoomImages(gymId, imageName, files));
    }

    @PutMapping(value={"/admin/gyms/{gymId}/rooms/images/{imageId}"}, consumes={"multipart/form-data"})
    @Operation(summary="Replace room image (Admin)", description="Replaces a specific interior image. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<GymImageDto> replaceRoomImage(@PathVariable Long gymId, @PathVariable Long imageId, @RequestParam(value="file") MultipartFile file) {
        return ResponseEntity.ok(this.gymService.replaceRoomImage(gymId, imageId, file));
    }

    @DeleteMapping(value={"/admin/gyms/{gymId}/rooms/images/{imageId}"})
    @Operation(summary="Delete room image (Admin)", description="Removes a specific interior image from a gym profile. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoomImage(@PathVariable Long gymId, @PathVariable Long imageId) {
        this.gymService.deleteRoomImage(gymId, imageId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long)principal;
        }
        return null;
    }

    public GymController(GymService gymService) {
        this.gymService = gymService;
    }
}

