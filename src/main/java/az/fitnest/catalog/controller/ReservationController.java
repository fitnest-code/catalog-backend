package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.model.entity.GymLessonType;
import az.fitnest.catalog.model.entity.ReservationRule;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.entity.TrainerReservationDate;
import az.fitnest.catalog.service.impl.CancellationReasonService;
import az.fitnest.catalog.service.impl.ReservationCommandService;
import az.fitnest.catalog.service.impl.ReservationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Pilates, Yoga və digər dərslər üçün rezervasiya idarəetmə ucluqları")
public class ReservationController {

    private final ReservationQueryService queryService;
    private final ReservationCommandService commandService;
    private final CancellationReasonService reasonService;

    @PostMapping("/lessons")
    @Operation(summary = "Dərs növlərini əldə edin", description = "Müəyyən kateqoriya üzrə aktiv dərs növlərini qaytarır.")
    public ResponseEntity<List<ReservationLessonResponse>> getLessons(@Valid @RequestBody ReservationLessonQueryRequest request) {
        return ResponseEntity.ok(queryService.getLessonsForReservation(request.getGymId(), request.getCategoryId()));
    }

    @PostMapping("/trainers")
    @Operation(summary = "Məşqçiləri əldə edin", description = "Seçilmiş dərs növü üzrə aktiv məşqçiləri qaytarır.")
    public ResponseEntity<List<ReservationTeacherResponse>> getTeachersByLesson(@Valid @RequestBody ReservationTeacherQueryRequest request) {
        return ResponseEntity.ok(queryService.getTrainersForLesson(request.getGymId(), request.getCategoryId(), request.getLessonTypeId()));
    }

    @PostMapping("/times")
    @Operation(summary = "Mövcud vaxtları əldə edin", description = "Məşqçi və dərs növü üzrə mövcud sessiyaları qaytarır.")
    public ResponseEntity<List<DayAvailabilityResponse>> getAvailableTimes(@Valid @RequestBody ReservationTimeQueryRequest request) {
        return ResponseEntity.ok(queryService.getAvailabilityForTeacher(request.getGymId(), request.getCategoryId(), request.getLessonTypeId(), request.getTrainerId()));
    }



    @PostMapping("/rules")
    @Operation(summary = "Rezervasiya qaydalarını əldə edin")
    public ResponseEntity<ReservationRuleResponse> getRules(@Valid @RequestBody ReservationRulesRequest request) {
        return ResponseEntity.ok(queryService.getRules(request.getGymId(), request.getCategoryId(), request.getLessonId()));
    }

    @PostMapping("/rules")
    @Operation(summary = "Rezervasiya qaydalarını yadda saxlayın")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> saveRules(@RequestBody ReservationRuleUpdateRequest request) {
        commandService.saveOrUpdateRules(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/preview")
    @Operation(summary = "Rezervasiya önbaxışı")
    public ResponseEntity<ReservationPreviewResponse> getPreview(@RequestBody ReservationPreviewRequest request) {
        return ResponseEntity.ok(queryService.getReservationPreview(request.getSessionId()));
    }

    @PostMapping("/my")
    @Operation(summary = "Mənim rezervasiyalarım")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal Long userId,
            @RequestBody ReservationListRequest request) {
        int page = request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
        List<ReservationResponse> items = queryService.getMyReservations(userId, page, pageSize);
        return ResponseEntity.ok(PaginatedResponse.<ReservationResponse>builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Rezervasiya təfərrüatları")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationDetailResponse> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(queryService.getReservationDetail(id, userId));
    }

    @GetMapping("/widget")
    @Operation(summary = "Rezervasiya vidceti məlumatları")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationWidgetResponse> getWidget(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(queryService.getReservationWidget(userId));
    }

    @GetMapping("/cancel-reasons")
    @Operation(summary = "Ləğvetmə səbəbləri")
    public ResponseEntity<List<CancelReasonResponse>> getCancelReasons() {
        return ResponseEntity.ok(reasonService.getActiveReasons());
    }

    @PostMapping
    @Operation(summary = "Rezervasiya yaradın")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReservationRequest request) {
        var reservation = commandService.createReservation(userId, request.getGymId(),
                request.getCategoryId(), request.getClassTypeId(), request.getTrainerId(), request.getSessionId());

        return ResponseEntity.ok(ReservationResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .trainerId(reservation.getTrainer().getId())
                .lessonType(reservation.getLessonType())
                .date(reservation.getReservationDate().getDate())
                .fromHour(reservation.getReservationDate().getStartTime())
                .toHour(reservation.getReservationDate().getEndTime())
                .status(reservation.getStatus())
                .build());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Rezervasiyanı ləğv edin")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @RequestBody ReservationCancelRequest request) {
        commandService.cancelReservation(id, userId, request.getReasonCode(), request.getAdditionalNote());
        return ResponseEntity.ok().build();
    }
}
