package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.service.impl.GymImageService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Gym Admin", description = "Administrative endpoints for managing gyms and trainers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class GymAdminController {

    private final GymWriteService gymWriteService;
    private final GymTrainerService gymTrainerService;
    private final GymImageService gymImageService;

    @PostMapping
    @Operation(summary = "Create gym (Admin)", description = "Creates a new gym profile. Requires ADMIN role.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Gym created successfully"), @ApiResponse(responseCode = "403", description = "Insufficient permissions")})
    public ResponseEntity<Void> createGym(@Valid @RequestBody GymRequest request) {
        this.gymWriteService.createGym(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{gymId}")
    @Operation(summary = "Update gym (Admin)", description = "Updates basic information of an existing gym. Requires ADMIN role.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Gym updated successfully"), @ApiResponse(responseCode = "404", description = "Gym not found")})
    public ResponseEntity<Void> updateGym(@PathVariable Long gymId, @Valid @RequestBody GymRequest request) {
        this.gymWriteService.updateGym(gymId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/subscriptions")
    @Operation(summary = "Update gym subscriptions (Admin)", description = "Updates the subscription plans and benefits linked to a gym. Requires ADMIN role.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Subscriptions updated successfully"), @ApiResponse(responseCode = "404", description = "Gym not found")})
    public ResponseEntity<Void> updateGymSubscriptions(@PathVariable Long gymId, @RequestBody GymSubscriptionsUpdateRequest request) {
        this.gymWriteService.updateGymSubscriptions(gymId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}")
    @Operation(summary = "Delete gym (Admin)", description = "Deletes a gym profile and all associated data. Requires ADMIN role.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Gym deleted successfully")})
    public ResponseEntity<Void> deleteGym(@PathVariable Long gymId) {
        this.gymWriteService.deleteGym(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gymId}/trainers")
    @Operation(summary = "Add trainer (Admin)", description = "Registers a new trainer for a specific gym. Requires ADMIN role.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Trainer added successfully")})
    public ResponseEntity<Void> addTrainer(@PathVariable Long gymId, @RequestBody TrainerRequest request) {
        this.gymTrainerService.addTrainer(gymId, request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{gymId}/trainers/{trainerId}")
    @Operation(summary = "Update trainer (Admin)", description = "Updates trainer information. Requires ADMIN role.")
    public ResponseEntity<Void> updateTrainer(@PathVariable Long gymId, @PathVariable Long trainerId, @RequestBody TrainerRequest request) {
        this.gymTrainerService.updateTrainer(gymId, trainerId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}/trainers/{trainerId}")
    @Operation(summary = "Delete trainer (Admin)", description = "Removes a trainer profile from a gym. Requires ADMIN role.")
    public ResponseEntity<Void> deleteTrainer(@PathVariable Long gymId, @PathVariable Long trainerId) {
        this.gymTrainerService.deleteTrainer(gymId, trainerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/logo")
    @Operation(summary = "Update logo (Admin)", description = "Uploads a new logo for the gym. Replaces the old one if it exists.")
    public ResponseEntity<Void> updateGymLogo(@PathVariable Long gymId, @RequestParam("file") MultipartFile file) {
        this.gymWriteService.updateLogo(gymId, file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}/logo")
    @Operation(summary = "Delete logo (Admin)", description = "Removes the logo image for a gym. Requires ADMIN role.")
    public ResponseEntity<Void> deleteGymLogo(@PathVariable Long gymId) {
        this.gymImageService.deleteLogoUrl(gymId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/cover")
    @Operation(summary = "Update cover image (Admin)", description = "Uploads a new cover image for the gym. Replaces the old one if it exists.")
    public ResponseEntity<Void> updateGymCover(@PathVariable Long gymId, @RequestParam("file") MultipartFile file) {
        this.gymWriteService.updateCoverImage(gymId, file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}/cover")
    @Operation(summary = "Delete cover image (Admin)", description = "Removes the cover image for a gym. Requires ADMIN role.")
    public ResponseEntity<Void> deleteGymCover(@PathVariable Long gymId) {
        this.gymImageService.deleteCoverImageUrl(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{gymId}/rooms/{roomName}/images", consumes = {"multipart/form-data"})
    @Operation(summary = "Upload room image (Admin)", description = "Uploads an interior image for a gym associated with a room. Requires ADMIN role.")
    public ResponseEntity<GymImageDto> uploadRoomImage(@PathVariable Long gymId, @PathVariable String roomName, @RequestParam(value = "file") MultipartFile file) {
        return ResponseEntity.ok(this.gymImageService.uploadRoomImage(gymId, roomName, file));
    }

    @DeleteMapping("/{gymId}/rooms/images/{imageId}")
    @Operation(summary = "Delete room image (Admin)", description = "Removes a specific interior image from a gym profile. Requires ADMIN role.")
    public ResponseEntity<Void> deleteRoomImage(@PathVariable Long gymId, @PathVariable Long imageId) {
        this.gymImageService.deleteRoomImage(gymId, imageId);
        return ResponseEntity.noContent().build();
    }
}
