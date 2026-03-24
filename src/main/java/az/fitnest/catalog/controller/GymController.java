package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.GymDetailResponse;
import az.fitnest.catalog.dto.GymImageDto;
import az.fitnest.catalog.dto.LocationDto;
import az.fitnest.catalog.dto.GymQrResponse;
import az.fitnest.catalog.dto.GymImageResponse;
import az.fitnest.catalog.dto.GymMainPageDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.GymCountResponse;
import az.fitnest.catalog.dto.GymTypeCountResponse;
import az.fitnest.catalog.dto.GymCategoryCountResponse;
import az.fitnest.catalog.dto.GymSubscriptionCountResponse;

import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymSubscriptionsUpdateRequest;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.ReviewRequest;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.dto.UpdateImageUrlRequest;
import az.fitnest.catalog.grpc.CreateGymRequest;
import az.fitnest.catalog.grpc.UpdateGymRequest;
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
import java.util.Set;
import java.util.stream.Collectors;

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

import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.mapper.GymProtoMapper;
import az.fitnest.catalog.dto.GymEntranceEligibilityResponse;
import az.fitnest.catalog.dto.GymEntranceScanResponse;

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
        String qrCodeUrl = this.gymReadService.getGymQrUrl(gymId);
        return ResponseEntity.ok(new GymQrResponse(qrCodeUrl));
    }

    @GetMapping("/{gymId}/trainers")
    @Operation(summary = "İdman zalı məşqçilərini əldə edin", description = "İdman zalında çalışan məşqçilərin səhifələnmiş siyahısını qaytarır.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Məşqçilər uğurla əldə edildi")})
    public ResponseEntity<PaginatedResponse<GymTrainerDto>> getTrainers(
            @PathVariable Long gymId,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size,
            @Parameter(description = "Sıralama istiqaməti. ASC (Artan sıra, A-dan Z-yə, ən aşağıdan ən yuxarıya), DESC (Azalan sıra, Z-dən A-ya, ən yuxarıdan ən aşağıya", schema = @Schema(implementation = SortDirection.class, example = "DESC")) @RequestParam(value = "sort_dir", defaultValue = "DESC") SortDirection sortDir) {
        int safePageSize = Math.min(page_size, 100);
        return ResponseEntity.ok(this.gymTrainerService.getTrainers(gymId, page, safePageSize, sortDir.name().toLowerCase()));
    }

    @GetMapping("/{gymId}/reviews")
    @Operation(summary = "İdman zalı rəylərini əldə edin", description = "İdman zalı üçün istifadəçi rəylərinin səhifələnmiş siyahısını qaytarır.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Rəylər uğurla əldə edildi")})
    public ResponseEntity<PaginatedResponse<GymReviewDto>> getReviews(@PathVariable Long gymId, @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size, @Parameter(description = "Çeşidləmə qaydası (məsələn, ən yeni, ən yüksək reytinq)") @RequestParam(required = false) String sort) {
        int safePageSize = Math.min(page_size, 100);
        return ResponseEntity.ok(this.gymReviewService.getReviews(gymId, page, safePageSize, sort));
    }

    @PostMapping("/{gymId}/reviews")
    @Operation(summary = "Rəy əlavə edin", description = "İdman zalı üçün yeni istifadəçi rəyi və reytinqi əlavə edir. Reytinq 1-5 arasında olmalıdır.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Rəy uğurla əlavə edildi"), @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb")})
    public ResponseEntity<Void> addReview(@AuthenticationPrincipal Object principal, @PathVariable Long gymId, @Valid @RequestBody ReviewRequest request) {
        Long userId = this.extractUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        this.gymReviewService.addReview(userId, gymId, request);
        return ResponseEntity.status(201).build();
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
            @Parameter(description = "Kateqoriya ID-si") @RequestParam(value = "category", required = false) Long categoryId,
            @Parameter(description = "Abunəlik ID-si") @RequestParam(value = "subscriptionId", required = false) Long subscriptionId,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size,
            @Parameter(description = "İstifadəçinin enliyi (latitude)") @RequestParam(value = "lat", required = false) Double lat,
            @Parameter(description = "İstifadəçinin uzunluğu (longitude)") @RequestParam(value = "lng", required = false) Double lng,
            @Parameter(description = "Sıralama istiqaməti. ASC (Artan sıra, A-dan Z-yə, ən aşağıdan ən yuxarıya), DESC (Azalan sıra, Z-dən A-ya, ən yuxarıdan ən aşağıya", schema = @Schema(implementation = SortDirection.class, example = "DESC")) @RequestParam(value = "sort_dir", defaultValue = "DESC") SortDirection sortDir) {
        Long userId = this.extractUserId(principal);
        int safePageSize = Math.min(page_size, 100);
        return ResponseEntity.ok(this.gymReadService.getGyms(userId, q, type, categoryId, subscriptionId, page, safePageSize, lat, lng, sortDir.name().toLowerCase()));
    }

    @PostMapping("/{gymId}/save")
    @Operation(summary = "İdman zalını saxla/sil", description = "Autentifikasiya olunmuş istifadəçi üçün idman zalının 'saxlanılanlar' statusunu dəyişir.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dəyişiklik uğurludur", content = {@Content(schema = @Schema(implementation = az.fitnest.catalog.dto.ToggleSaveResponse.class))}),
            @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı", content = {@Content(schema = @Schema(implementation = az.fitnest.catalog.dto.ApiError.class))}),
            @ApiResponse(responseCode = "401", description = "İstifadəçi autentifikasiya olunmayıb", content = {@Content(schema = @Schema(implementation = az.fitnest.catalog.dto.ApiError.class))})
    })
    public ResponseEntity<az.fitnest.catalog.dto.ToggleSaveResponse> toggleSave(@AuthenticationPrincipal Object principal, @PathVariable Long gymId) {
        Long userId = this.extractUserId(principal);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean isSaved = this.gymWriteService.toggleSave(userId, gymId);
        return ResponseEntity.ok(new az.fitnest.catalog.dto.ToggleSaveResponse(isSaved));
    }

    @GetMapping("/{gymId}/location")
    @Operation(summary = "İdman zalının yerləşdiyi yeri əldə edin", description = "İdman zalı üçün həll edilmiş ünvan mətni ilə birlikdə enlik və uzunluq koordinatlarını qaytarır.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Məkan uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = LocationDto.class))}), @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı")})
    public ResponseEntity<LocationDto> getGymLocation(@Parameter(description = "İdman zalının ID-si") @PathVariable Long gymId) {
        return ResponseEntity.ok(this.gymReadService.getGymLocation(gymId));
    }

    @PostMapping("/entrance/scan")
    @Operation(summary = "Scan gym QR code for entrance (proximity only)", description = "Checks if user is close to the gym location. Returns gym name, address, enterDate, enterHour, notAllowed.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proximity check result", content = @Content(schema = @Schema(implementation = az.fitnest.catalog.dto.GymEntranceScanResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not close enough", content = @Content(schema = @Schema(implementation = az.fitnest.catalog.dto.ApiError.class)))
    })
    public ResponseEntity<GymEntranceScanResponse> scanGymQrEntrance(
            @AuthenticationPrincipal Object principal,
            @RequestParam String qrCodeValue,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        try {
            var response = gymReadService.scanGymQrEntrance(principal, qrCodeValue, lat, lng);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/entrance/eligibility")
    @Operation(summary = "Check gym entrance eligibility", description = "Checks entry limit, subscription status, and other eligibility criteria for gym entrance.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Eligibility check result", content = @Content(schema = @Schema(implementation = az.fitnest.catalog.dto.GymEntranceResponse.class))),
        @ApiResponse(responseCode = "403", description = "Not eligible", content = @Content(schema = @Schema(implementation = az.fitnest.catalog.dto.ApiError.class)))
    })
    public ResponseEntity<?> checkGymEntranceEligibility(
            @AuthenticationPrincipal Object principal) {
        try {
            var response = gymReadService.checkGymEntranceEligibility(principal);
            if (response.allowed()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(403).body(response);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/count")
    @Operation(summary = "Get gym count", description = "Returns count of gyms filtered by type (all, new), subscriptionId, and categoryId.")
    public ResponseEntity<GymCountResponse> getGymCount(
            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
            @RequestParam(value = "subscriptionId", required = false) Long subscriptionId,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        GymCountResponse response = gymReadService.getGymCount(type, subscriptionId, categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/by-type")
    @Operation(summary = "Get gym count by type", description = "Returns count of gyms for the requested type (all, new).")
    public ResponseEntity<GymTypeCountResponse> getGymCountByType(@RequestParam(value = "type", required = false, defaultValue = "all") String type) {
        return ResponseEntity.ok(gymReadService.getGymCountByType(type));
    }

    @GetMapping("/count/by-category")
    @Operation(summary = "Get gym count by category", description = "Returns all categories with their gym counts.")
    public ResponseEntity<List<GymCategoryCountResponse>> getGymCountByCategory() {
        return ResponseEntity.ok(gymReadService.getGymCountByCategory());
    }

    @GetMapping("/count/by-subscription")
    @Operation(summary = "Get gym count by subscription", description = "Returns all subscriptions with their gym counts.")
    public ResponseEntity<List<GymSubscriptionCountResponse>> getGymCountBySubscription() {
        return ResponseEntity.ok(gymReadService.getGymCountBySubscription());
    }

    @GetMapping("/count/by-gender")
    @Operation(summary = "Get gym count by gender", description = "Returns count of gyms with gender-specific work hours (male/female).")
    public ResponseEntity<GymTypeCountResponse> getGymCountByGender(@RequestParam("gender") String gender) {
        return ResponseEntity.ok(gymReadService.getGymCountByGender(gender));
    }

    public GymEntranceEligibilityResponse testEligibilityResponse() {
        return new GymEntranceEligibilityResponse(true);
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    private az.fitnest.catalog.dto.GymRequest mapToGymRequest(CreateGymRequest request) {
        return GymProtoMapper.mapToGymRequest(request);
    }

    private az.fitnest.catalog.dto.GymRequest mapToGymRequest(UpdateGymRequest request) {
        return GymProtoMapper.mapToGymRequest(request);
    }

    private Long extractGymIdFromQr(String qrCodeValue) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/gym/(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(qrCodeValue);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
