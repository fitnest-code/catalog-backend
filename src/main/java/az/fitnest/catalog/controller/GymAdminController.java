package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymSubscriptionBenefitsUpdateRequest;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.service.impl.GymWriteService;
import az.fitnest.catalog.service.impl.GymReviewService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import az.fitnest.catalog.service.impl.ReservationService;
import az.fitnest.catalog.service.impl.GymLessonTypeService;

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Gym Admin", description = "İdman zallarını idarə etmək üçün administrativ ucluqlar. Bu ucluqlar yalnız ADMIN və SUPER_ADMIN rollarına malik istifadəçilər tərəfindən istifadə edilə bilər.")
@SecurityRequirement(name = "bearerAuth")
public class GymAdminController {

    private final GymWriteService gymWriteService;
    private final GymReviewService gymReviewService;
    private final GymTrainerService gymTrainerService;
    private final ReservationService reservationService;
    private final GymLessonTypeService gymLessonTypeService;

    @Operation(summary = "Yeni idman zalı yaradın", description = "Sistemə yeni idman zalı əlavə edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Void> createGym(@Valid @RequestBody GymRequest request) {
        gymWriteService.createGym(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "İdman zalını yeniləyin", description = "Mövcud idman zalının məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateGym(@PathVariable Long id, @Valid @RequestBody GymRequest request) {
        gymWriteService.updateGym(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalı üçün mövcud abunəlik planlarını aktivləşdirin", description = "İdman zalı üçün göstərilən abunəlik planlarını aktivləşdirir. Mövcud planların üstünlükləri qorunur, siyahıda olmayanlar isə silinir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/subscriptions/enable")
    public ResponseEntity<Void> enableGymSubscription(
            @PathVariable("id") Long gymId,
            @RequestParam("subscriptionId") Long subscriptionId) {
        gymWriteService.enableGymSubscription(gymId, subscriptionId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Aktivləşdirilmiş abunəlik üçün üstünlükləri yeniləyin", description = "İdman zalı üçün artıq aktivləşdirilmiş olan müəyyən bir abunəliyin üstünlüklərini (benefits) yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/subscriptions/{planId}/benefits")
    public ResponseEntity<Void> updateGymSubscriptionBenefits(@PathVariable Long id, @PathVariable Long planId, @Valid @RequestBody GymSubscriptionBenefitsUpdateRequest request) {
        gymWriteService.updateGymSubscriptionBenefits(id, planId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalını silin", description = "İdman zalını sistemdən silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGym(@PathVariable Long id) {
        gymWriteService.deleteGym(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalına otaq şəkli əlavə edin", description = "İdman zalı üçün otaq şəkilləri yükləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addRoomImages(
            @PathVariable Long id,
            @RequestParam("roomNames") java.util.List<String> roomNames,
            @RequestParam("files") java.util.List<MultipartFile> files) {
        if (roomNames == null || files == null || roomNames.size() != files.size()) {
            return ResponseEntity.badRequest().build();
        }
        gymWriteService.addRoomImages(id, roomNames, files);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalının bütün otaqlarını silin", description = "İdman zalı üçün bütün otaqları və onların şəkillərini silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/rooms")
    public ResponseEntity<Void> deleteAllGymRooms(@PathVariable Long id) {
        gymWriteService.deleteAllGymRooms(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalının müəyyən bir otağını silin", description = "İdman zalı üçün göstərilən otağı və onun şəkillərini silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/rooms/{roomId}")
    public ResponseEntity<Void> deleteGymRoomById(@PathVariable Long id, @PathVariable Long roomId) {
        gymWriteService.deleteGymRoomById(id, roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalının müəyyən bir otaq şəklini silin", description = "İdman zalının otaqlarına aid olan müəyyən bir şəkli silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/rooms/images/{imageId}")
    public ResponseEntity<Void> deleteRoomImageById(@PathVariable Long id, @PathVariable Long imageId) {
        gymWriteService.deleteRoomImageById(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalı üz qabığı şəklini yeniləyin", description = "İdman zalı üçün əsas profil şəklini yükləyir və ya yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCoverImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        gymWriteService.updateCoverImage(id, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün idman zallarını silin (Kritik)", description = "Sistemdəki BÜTÜN idman zallarını silir. Bu əməliyyat üçün SUPER_ADMIN rolu tələb olunur və bu hərəkət geri qaytarıla bilməz.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllGyms() {
        gymWriteService.deleteAllGyms();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mövcud olmayan və ya təsdiqlənməmiş rəyi təsdiqləyin", description = "İdman zalına verilmiş rəyi təsdiqləyir və ümumi reytinqə daxil edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<Void> approveReview(@PathVariable Long reviewId) {
        gymReviewService.approveReview(reviewId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rəyi rədd edin", description = "İdman zalına verilmiş rəyi rədd edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reviews/{reviewId}/reject")
    public ResponseEntity<Void> rejectReview(@PathVariable Long reviewId) {
        gymReviewService.rejectReview(reviewId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Təsdiq gözləyən rəyləri alın", description = "Sistemdə təsdiq gözləyən (PENDING) rəylərin siyahısını gətirir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reviews/pending")
    public ResponseEntity<PaginatedResponse<GymReviewDto>> getPendingReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(gymReviewService.getPendingReviews(page, pageSize));
    }

    @Operation(summary = "Bütün abunəlikləri silin", description = "İdman zalı üçün bütün abunəlikləri silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/subscriptions")
    public ResponseEntity<Void> deleteAllGymSubscriptions(@PathVariable("id") Long gymId) {
        gymWriteService.deleteAllGymSubscriptions(gymId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Abunəliyi silin", description = "İdman zalı üçün müəyyən bir abunəliyi silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/subscriptions/{subscriptionId}")
    public ResponseEntity<Void> deleteGymSubscriptionById(@PathVariable("id") Long gymId, @PathVariable("subscriptionId") Long subscriptionId) {
        gymWriteService.deleteGymSubscriptionById(gymId, subscriptionId);
        return ResponseEntity.noContent().build();
    }
    @Operation(summary = "İdman zalı üçün təlimçi yaradın", description = "İdman zalına yeni təlimçi əlavə edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/trainers")
    public ResponseEntity<Void> addTrainer(@PathVariable("id") Long gymId, @Valid @RequestBody TrainerRequest request) {
        gymTrainerService.addTrainer(gymId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Təlimçini yeniləyin", description = "İdman zalına aid təlimçinin məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/trainers/{trainerId}")
    public ResponseEntity<Void> updateTrainer(@PathVariable("id") Long gymId, @PathVariable("trainerId") Long trainerId, @Valid @RequestBody TrainerRequest request) {
        gymTrainerService.updateTrainer(gymId, trainerId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Təlimçini silin", description = "İdman zalına aid təlimçini silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/trainers/{trainerId}")
    public ResponseEntity<Void> deleteTrainer(@PathVariable("id") Long gymId, @PathVariable("trainerId") Long trainerId) {
        gymTrainerService.deleteTrainer(gymId, trainerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Enable or disable reservations for a gym", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reservation/enable")
    public ResponseEntity<Void> toggleGymReservation(@PathVariable Long id, @RequestParam boolean enabled) {
        gymWriteService.toggleGymReservation(id, enabled);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Enable or disable reservations for a trainer", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/trainers/{trainerId}/reservation/enable")
    public ResponseEntity<Void> toggleTrainerReservation(@PathVariable("id") Long gymId, @PathVariable("trainerId") Long trainerId, @RequestParam boolean enabled) {
        gymTrainerService.toggleTrainerReservation(gymId, trainerId, enabled);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Add availability for trainer", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/trainers/{trainerId}/availabilities")
    public ResponseEntity<Void> addTrainerAvailability(
            @PathVariable("id") Long gymId, 
            @PathVariable("trainerId") Long trainerId, 
            @Valid @RequestBody az.fitnest.catalog.dto.TrainerAvailabilityRequest request) {
        gymTrainerService.addTrainerAvailability(gymId, trainerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update reservation status", description = "ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reservations/{reservationId}/status")
    public ResponseEntity<Void> updateReservationStatus(
            @PathVariable Long reservationId,
            @Valid @RequestBody az.fitnest.catalog.dto.ReservationStatusUpdateRequest request) {
        reservationService.updateReservationStatus(reservationId, request);
        return ResponseEntity.ok().build();
    }

    // ==================== Lesson Type CRUD ====================

    @Operation(summary = "Add lesson type to gym", description = "Adds a new lesson type (e.g. Fly Yoga, Hatha Yoga) to the gym. ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/lesson-types")
    public ResponseEntity<az.fitnest.catalog.dto.GymLessonTypeResponse> addLessonType(
            @PathVariable("id") Long gymId,
            @Valid @RequestBody az.fitnest.catalog.dto.GymLessonTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymLessonTypeService.addLessonType(gymId, request));
    }

    @Operation(summary = "Get all lesson types for a gym", description = "Returns all lesson types configured for the gym. ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/lesson-types")
    public ResponseEntity<java.util.List<az.fitnest.catalog.dto.GymLessonTypeResponse>> getLessonTypes(@PathVariable("id") Long gymId) {
        return ResponseEntity.ok(gymLessonTypeService.getLessonTypes(gymId));
    }

    @Operation(summary = "Update a lesson type", description = "Updates the name of an existing lesson type. ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/lesson-types/{lessonTypeId}")
    public ResponseEntity<az.fitnest.catalog.dto.GymLessonTypeResponse> updateLessonType(
            @PathVariable("id") Long gymId,
            @PathVariable Long lessonTypeId,
            @Valid @RequestBody az.fitnest.catalog.dto.GymLessonTypeRequest request) {
        return ResponseEntity.ok(gymLessonTypeService.updateLessonType(gymId, lessonTypeId, request));
    }

    @Operation(summary = "Delete a lesson type", description = "Removes a lesson type from the gym. ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/lesson-types/{lessonTypeId}")
    public ResponseEntity<Void> deleteLessonType(
            @PathVariable("id") Long gymId,
            @PathVariable Long lessonTypeId) {
        gymLessonTypeService.deleteLessonType(gymId, lessonTypeId);
        return ResponseEntity.noContent().build();
    }
}
