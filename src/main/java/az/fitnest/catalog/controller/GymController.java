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
    @Tag(name = "Gyms", description = "İdman zallarını idarə etmək və kəşf etmək üçün ucluqlar, o cümlədən paketlər, məşqçilər və istifadəçi rəyləri")
    public class GymController {
        private final GymReadService gymReadService;
        private final GymWriteService gymWriteService;
        private final GymImageService gymImageService;
        private final GymReviewService gymReviewService;
        private final GymTrainerService gymTrainerService;

        @GetMapping("/{gymId:\\d+}")
        @Operation(summary = "İdman zalı təfərrüatlarını əldə edin", description = "Xüsusi bir idman zalının tam təfərrüatlarını, o cümlədən yerləşmə yeri, obyektləri və istifadəçiyə xüsusi favorit statusunu əldə edir.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "İdman zalı təfərrüatları uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = GymDetailResponse.class))}), @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı")})
        public ResponseEntity<GymDetailResponse> getGymDetail(@AuthenticationPrincipal Object principal, @Parameter(description = "İdman zalının ID-si") @PathVariable Long gymId) {
            Long userId = this.extractUserId(principal);
            return ResponseEntity.ok(this.gymReadService.getGymDetail(userId, gymId));
        }

        @GetMapping("/{gymId}/images")
        @Operation(summary = "İdman zalı şəkillərini əldə edin", description = "İdman zalı ilə əlaqəli bütün şəkillərin (loqo, üz qabığı, interyer) siyahısını qaytarır.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Şəkillər uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = GymImageResponse.class))})})
        public ResponseEntity<GymImageResponse> getGymImages(@PathVariable Long gymId) {
            return ResponseEntity.ok(this.gymReadService.getGymImages(gymId));
        }

        @GetMapping("/{gymId}/qr")
        @Operation(summary = "İdman zalı QR kod URL-ni əldə edin", description = "İdman zalının QR kod şəkli üçün yayım URL-ni qaytarır.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "QR kod URL-i uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = GymQrResponse.class))})})
        public ResponseEntity<GymQrResponse> getGymQrUrl(@Parameter(description = "İdman zalının ID-si") @PathVariable Long gymId) {
            String qrCodeUrl = this.gymReadService.getGymDetail(null, gymId).getQr_code_url();
            return ResponseEntity.ok(new GymQrResponse(qrCodeUrl));
        }


        @GetMapping("/{gymId}/trainers")
        @Operation(summary = "İdman zalı məşqçilərini əldə edin", description = "İdman zalında çalışan məşqçilərin səhifələnmiş siyahısını qaytarır.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Məşqçilər uğurla əldə edildi")})
        public ResponseEntity<PaginatedResponse<GymTrainerDto>> getTrainers(@PathVariable Long gymId, @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size) {
            return ResponseEntity.ok(this.gymTrainerService.getTrainers(gymId, page, page_size));
        }

        @GetMapping("/{gymId}/reviews")
        @Operation(summary = "İdman zalı rəylərini əldə edin", description = "İdman zalı üçün istifadəçi rəylərinin səhifələnmiş siyahısını qaytarır.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Rəylər uğurla əldə edildi")})
        public ResponseEntity<PaginatedResponse<GymReviewDto>> getReviews(@PathVariable Long gymId, @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size, @Parameter(description = "Çeşidləmə qaydası (məsələn, ən yeni, ən yüksək reytinq)") @RequestParam(required = false) String sort) {
            return ResponseEntity.ok(this.gymReviewService.getReviews(gymId, page, page_size, sort));
        }

        @PostMapping("/{gymId}/reviews")
        @Operation(summary = "Rəy bildirin", description = "Autentifikasiya olunmuş istifadəçiyə idman zalı üçün reytinq və şərh yazmağa imkan verir.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Rəy uğurla göndərildi"), @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb"), @ApiResponse(responseCode = "400", description = "Yanlış reytinq şkalası (1-5)")})
        public ResponseEntity<Void> addReview(@AuthenticationPrincipal Object principal, @PathVariable Long gymId, @Valid @RequestBody ReviewRequest request) {
            Long userId = this.extractUserId(principal);
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }
            this.gymReviewService.addReview(userId, gymId, request);
            return ResponseEntity.status(201).build();
        }

        @PostMapping("/{gymId}/check-in")
        @Operation(summary = "İdman zalına giriş (check-in)", description = "Autentifikasiya olunmuş istifadəçiyə idman zalının QR kodu vasitəsilə giriş etməyə imkan verir. Aktiv abunəlikdən bir giriş çıxılır.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Giriş uğurludur", content = {@Content(schema = @Schema(implementation = az.fitnest.catalog.dto.CheckInResponseDto.class))}),
                @ApiResponse(responseCode = "400", description = "Giriş uğursuz oldu (məsələn, aktiv abunəlik yoxdur və ya müddəti bitib)"),
                @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb")
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
        @Operation(summary = "Rezervasiya qaydalarını əldə edin", description = "İdman zalına giriş və rezervasiya üçün xüsusi qaydaları qaytarır (məsələn, vaxt məhdudiyyətləri, ləğv siyasəti).")
        public ResponseEntity<Object> getReservationRules(@PathVariable Long gymId) {
            return ResponseEntity.ok(this.gymReadService.getReservationRules(gymId));
        }

        @GetMapping
        @Operation(summary = "İdman zallarını əldə edin", description = "Bütün idman zalı siyahıları üçün birləşdirilmiş ucluq (Hamısı, Ən Yaxın, Yeni, Saxlanılanlar). Görünüşləri dəyişmək üçün 'type' parametrindən istifadə edin.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "İdman zalları uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))})})
        public ResponseEntity<PaginatedResponse<GymMainPageDto>> getGyms(
                @AuthenticationPrincipal Object principal,
                @Parameter(description = "Axtarış sorğusu") @RequestParam(value = "q", required = false) String q,
                @Parameter(description = "Filtr növü (ALL, NEW, CLOSEST, SAVED)") @RequestParam(value = "type", defaultValue = "ALL") String type,
                @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
                @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size,
                @Parameter(description = "İstifadəçinin enliyi (latitude)") @RequestParam(value = "lat", required = false) Double lat,
                @Parameter(description = "İstifadəçinin uzunluğu (longitude)") @RequestParam(value = "lng", required = false) Double lng) {
            Long userId = this.extractUserId(principal);
            return ResponseEntity.ok(this.gymReadService.getGyms(userId, q, type, page, page_size, lat, lng));
        }

        @PostMapping("/{gymId}/save")
        @Operation(summary = "İdman zalını saxla/sil", description = "Autentifikasiya olunmuş istifadəçi üçün idman zalının 'saxlanılanlar' statusunu dəyişir.")
        @SecurityRequirement(name = "bearerAuth")
        @PreAuthorize("isAuthenticated()")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Dəyişiklik uğurludur"),
                @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı"),
                @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb")
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
        @Operation(summary = "İdman zalının yerləşdiyi yeri əldə edin", description = "İdman zalı üçün həll edilmiş ünvan mətni ilə birlikdə enlik və uzunluq koordinatlarını qaytarır.")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Məkan uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = LocationDto.class))}), @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı")})
        public ResponseEntity<LocationDto> getGymLocation(@Parameter(description = "İdman zalının ID-si") @PathVariable Long gymId) {
            return ResponseEntity.ok(this.gymReadService.getGymLocation(gymId));
        }

        private Long extractUserId(Object principal) {
            if (principal instanceof Long) {
                return (Long) principal;
            }
            return null;
        }
    }

