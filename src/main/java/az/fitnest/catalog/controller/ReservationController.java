package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.model.enums.ReservationStatus;
import az.fitnest.catalog.service.CancellationReasonService;
import az.fitnest.catalog.service.ReservationCommandService;
import az.fitnest.catalog.service.ReservationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Pilates, Yoga və digər dərslər üçün rezervasiya idarəetmə ucluqları")
public class ReservationController {

    private final ReservationQueryService queryService;
    private final ReservationCommandService commandService;
    private final CancellationReasonService reasonService;

    @GetMapping("/lessons")
    @Operation(summary = "Dərs növlərini əldə edin", description = "Müəyyən kateqoriya üzrə aktiv dərs növlərini qaytarır.")
    public ResponseEntity<List<ReservationLessonResponse>> getLessons(
            @RequestParam Long gymId,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(queryService.getLessonsForReservation(gymId, categoryId));
    }

    @GetMapping("/trainers")
    @Operation(summary = "Məşqçiləri əldə edin", description = "Seçilmiş dərs növü üzrə aktiv məşqçiləri qaytarır.")
    public ResponseEntity<List<ReservationTeacherResponse>> getTeachersByLesson(
            @RequestParam Long gymId,
            @RequestParam Long categoryId,
            @RequestParam Long lessonTypeId) {
        return ResponseEntity.ok(queryService.getTrainersForLesson(gymId, categoryId, lessonTypeId));
    }

    @GetMapping("/times")
    @Operation(summary = "Mövcud vaxtları əldə edin", description = "Məşqçi və dərs növü üzrə mövcud sessiyaları qaytarır.")
    public ResponseEntity<List<DayAvailabilityResponse>> getAvailableTimes(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long gymId,
            @RequestParam Long categoryId,
            @RequestParam Long lessonTypeId,
            @RequestParam Long trainerId) {
        return ResponseEntity.ok(queryService.getAvailabilityForTeacher(userId, gymId, categoryId, lessonTypeId, trainerId));
    }

    @GetMapping("/rules")
    @Operation(summary = "Rezervasiya qaydalarını əldə edin")
    public ResponseEntity<ReservationRuleResponse> getRules(
            @RequestParam Long gymId,
            @RequestParam Long categoryId,
            @RequestParam Long lessonId) {
        return ResponseEntity.ok(queryService.getRules(gymId, categoryId, lessonId));
    }

    @GetMapping("/gyms/{id}/status")
    @Operation(summary = "İdman zalının rezervasiya statusunu əldə edin")
    public ResponseEntity<GymReservationStatusResponse> getGymStatus(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getGymReservationStatus(id));
    }

    @PostMapping("/preview")
    @Operation(summary = "Rezervasiya önbaxışı")
    public ResponseEntity<ReservationPreviewResponse> getPreview(@RequestBody ReservationPreviewRequest request) {
        return ResponseEntity.ok(queryService.getReservationPreview(request.getSessionId()));
    }

    @GetMapping("/my")
    @Operation(summary = "Mənim rezervasiyalarım")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(
                in = ParameterIn.QUERY,
                description = "Filter by reservation status. Accepted values: PENDING, APPROVED, CANCELLED, REJECTED, EXPIRED",
                schema = @Schema(type = "string", allowableValues = {"PENDING", "APPROVED", "CANCELLED", "REJECTED", "EXPIRED"})
            )
            @RequestParam(required = false) ReservationStatus status) {
        int p = page > 0 ? page : 1;
        int ps = pageSize > 0 ? pageSize : 10;
        List<ReservationResponse> items = queryService.getMyReservations(userId, p, ps, status);
        return ResponseEntity.ok(PaginatedResponse.<ReservationResponse>builder()
                .items(items)
                .page(p)
                .pageSize(ps)
                .build());
    }

    @GetMapping("/cancel-reasons")
    @Operation(summary = "Ləğvetmə səbəbləri")
    public ResponseEntity<List<CancelReasonResponse>> getCancelReasons() {
        return ResponseEntity.ok(reasonService.getReasons());
    }

    @PostMapping
    @Operation(summary = "Rezervasiya yaradın")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReservationRequest request) {
        var reservation = commandService.createReservation(userId, request.getGymId(),
                request.getCategoryId(), request.getLessonId(), request.getTrainerId(), request.getSessionId());

        return ResponseEntity.ok(ReservationResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .gymName(reservation.getGym().getName())
                .trainerId(reservation.getTrainer().getId())
                .lessonType(reservation.getLessonType())
                .categoryName(reservation.getCategory() != null ? reservation.getCategory().getName() : null)
                .date(reservation.getReservationDate().getDate())
                .fromHour(reservation.getReservationDate().getStartTime())
                .toHour(reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .sessionId(reservation.getReservationDate().getId())
                .build());
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    @Operation(summary = "Rezervasiyanı seansa görə ləğv edin")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId,
            @RequestBody ReservationCancelRequest request) {
        commandService.cancelReservation(sessionId, userId, request.getReasonCode(), request.getAdditionalNote());
        return ResponseEntity.ok().build();
    }
}
