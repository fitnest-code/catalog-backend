package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.service.impl.CancellationReasonService;
import az.fitnest.catalog.service.impl.GymLessonTypeService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymWriteService;
import az.fitnest.catalog.service.impl.ReservationCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Admin", description = "Rezervasiyaları, təlimçiləri və dərs növlərini idarə etmək üçün administrativ ucluqlar.")
@SecurityRequirement(name = "bearerAuth")
public class ReservationAdminController {

    private final GymWriteService gymWriteService;
    private final GymTrainerService gymTrainerService;
    private final ReservationCommandService reservationCommandService;
    private final GymLessonTypeService gymLessonTypeService;
    private final CancellationReasonService cancellationReasonService;

    @Operation(summary = "İdman zalı üçün rezervasiyaları aktivləşdirin və ya deaktiv edin")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/gyms/{id}/enable")
    public ResponseEntity<Void> toggleGymReservation(@PathVariable Long id, @RequestBody ToggleRequest request) {
        gymWriteService.toggleGymReservation(id, request.isEnabled());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Təlimçi üçün rezervasiyaları aktivləşdirin və ya deaktiv edin")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/gyms/{id}/trainers/{trainerId}/enable/{lessonId}")
    public ResponseEntity<Void> toggleTrainerReservation(
            @PathVariable("id") Long gymId,
            @PathVariable("trainerId") Long trainerId,
            @PathVariable("lessonId") Long lessonId,
            @RequestBody ToggleRequest request) {
        gymTrainerService.toggleTrainerReservation(gymId, trainerId, request.isEnabled(), lessonId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Təlimçi üçün mövcudluq (availability) əlavə edin")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/gyms/{id}/trainers/{trainerId}/lessons/{lessonId}/availabilities")
    public ResponseEntity<Void> addTrainerAvailability(
            @PathVariable("id") Long gymId,
            @PathVariable("trainerId") Long trainerId,
            @PathVariable("lessonId") Long lessonId,
            @Valid @RequestBody TrainerAvailabilityRequest request) {
        gymTrainerService.addTrainerAvailability(gymId, trainerId, lessonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Rezervasiya statusunu yeniləyin")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{reservationId}/status")
    public ResponseEntity<Void> updateReservationStatus(
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationStatusUpdateRequest request) {
        reservationCommandService.updateReservationStatus(reservationId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalına dərs növü əlavə edin")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/gyms/{id}/lesson-types")
    public ResponseEntity<GymLessonTypeResponse> addLessonType(
            @PathVariable("id") Long gymId,
            @RequestParam(required = false) Long categoryId,
            @Valid @RequestBody GymLessonTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymLessonTypeService.addLessonType(gymId, categoryId, request));
    }

    @Operation(summary = "İdman zalının bütün dərs növlərini əldə edin")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/gyms/{id}/lesson-types")
    public ResponseEntity<List<GymLessonTypeResponse>> getLessonTypes(@PathVariable("id") Long gymId) {
        return ResponseEntity.ok(gymLessonTypeService.getLessonTypes(gymId));
    }

    @Operation(summary = "Dərs növünü yeniləyin")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/gyms/{id}/lesson-types/{lessonTypeId}")
    public ResponseEntity<GymLessonTypeResponse> updateLessonType(
            @PathVariable("id") Long gymId,
            @PathVariable Long lessonTypeId,
            @RequestParam(required = false) Long categoryId,
            @Valid @RequestBody GymLessonTypeRequest request) {
        return ResponseEntity.ok(gymLessonTypeService.updateLessonType(gymId, lessonTypeId, categoryId, request));
    }

    @Operation(summary = "Dərs növünü silin")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/gyms/{id}/lesson-types/{lessonTypeId}")
    public ResponseEntity<Void> deleteLessonType(
            @PathVariable("id") Long gymId,
            @PathVariable Long lessonTypeId) {
        gymLessonTypeService.deleteLessonType(gymId, lessonTypeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rezervasiya qaydalarını yadda saxlayın")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/gyms/{gymId}/categories/{categoryId}/lessons/{lessonId}/rules")
    public ResponseEntity<Void> saveRules(
            @PathVariable Long gymId,
            @PathVariable Long categoryId,
            @PathVariable Long lessonId,
            @RequestBody ReservationRuleUpdateRequest request) {
        reservationCommandService.saveOrUpdateRules(gymId, categoryId, lessonId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rezervasiya ləğv səbəblərini əldə edin")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cancel-reasons")
    public ResponseEntity<List<CancelReasonResponse>> getAllCancelReasons() {
        return ResponseEntity.ok(cancellationReasonService.getReasons());
    }

    @Operation(summary = "Yeni rezervasiya ləğv səbəbi əlavə edin")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cancel-reasons")
    public ResponseEntity<Void> createCancelReason(@Valid @RequestBody CancelReasonRequest request) {
        cancellationReasonService.createReason(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Rezervasiya ləğv səbəbini yeniləyin")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/cancel-reasons/{code}")
    public ResponseEntity<Void> updateCancelReason(@PathVariable String code, @Valid @RequestBody CancelReasonRequest request) {
        cancellationReasonService.updateReason(code, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rezervasiya ləğv səbəbini silin")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/cancel-reasons/{code}")
    public ResponseEntity<Void> deleteCancelReason(@PathVariable String code) {
        cancellationReasonService.deleteReason(code);
        return ResponseEntity.noContent().build();
    }
}
