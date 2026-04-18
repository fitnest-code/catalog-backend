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

    @GetMapping("/entry")
    @Operation(summary = "Rezervasiya giriş məlumatlarını əldə edin", description = "Müəyyən kateqoriya üzrə aktiv dərs növlərini qaytarır.")
    public ResponseEntity<List<GymLessonType>> getEntryData(
            @RequestParam Long gymId,
            @RequestParam String categoryName) {
        return ResponseEntity.ok(queryService.getReservationEntryData(gymId, categoryName));
    }

    @GetMapping("/trainers")
    @Operation(summary = "Məşqçiləri əldə edin", description = "Seçilmiş tarix və dərs növü üzrə aktiv məşqçiləri qaytarır.")
    public ResponseEntity<List<Trainer>> getTrainers(
            @RequestParam Long gymId,
            @RequestParam Long classTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(queryService.getTrainers(gymId, classTypeId, date));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Sessiyaları əldə edin", description = "Məşqçi və tarix üzrə mövcud sessiyaları qaytarır.")
    public ResponseEntity<List<TrainerReservationDate>> getSessions(
            @RequestParam Long gymId,
            @RequestParam Long classTypeId,
            @RequestParam Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(queryService.getSessions(gymId, classTypeId, trainerId, date));
    }

    @GetMapping("/rules")
    @Operation(summary = "Rezervasiya qaydalarını əldə edin")
    public ResponseEntity<List<ReservationRule>> getRules(
            @RequestParam Long gymId,
            @RequestParam String categoryName) {
        return ResponseEntity.ok(queryService.getRules(gymId, categoryName));
    }

    @GetMapping("/preview")
    @Operation(summary = "Rezervasiya önbaxışı")
    public ResponseEntity<ReservationPreviewResponse> getPreview(@RequestParam Long sessionId) {
        return ResponseEntity.ok(queryService.getReservationPreview(sessionId));
    }

    @GetMapping("/my")
    @Operation(summary = "Mənim rezervasiyalarım")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
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
                request.getCategoryName(), request.getClassTypeId(), request.getTrainerId(), request.getSessionId());

        return ResponseEntity.ok(ReservationResponse.builder()
                .id(reservation.getId())
                .gymId(reservation.getGym().getId())
                .trainerId(reservation.getTrainer().getId())
                .lessonType(reservation.getLessonType())
                .date(reservation.getReservationDate().getDate())
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
            @RequestParam String reasonCode,
            @RequestParam(required = false) String additionalNote) {
        commandService.cancelReservation(id, userId, reasonCode, additionalNote);
        return ResponseEntity.ok().build();
    }
}
