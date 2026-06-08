package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.dto.response.GymDetailResponseV2;
import az.fitnest.catalog.dto.response.GymMainPageResponseV2;
import az.fitnest.catalog.service.GymReadService;
import az.fitnest.catalog.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/gyms")
@RequiredArgsConstructor
@Tag(name = "Gyms V2", description = "İdman zallarını idarə etmək və kəşf etmək üçün V2 ucluqlar")
public class GymV2Controller {
    private final GymReadService gymReadService;

    @GetMapping("/{gymId:\\d+}")
    @Operation(summary = "İdman zalı təfərrüatlarını əldə edin (V2)", description = "Xüsusi bir idman zalının tam təfərrüatlarını (çoxlu kateqoriya dəstəyi ilə) əldə edir.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İdman zalı təfərrüatları uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = GymDetailResponseV2.class))}),
            @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı")
    })
    public ResponseEntity<GymDetailResponseV2> getGymDetail(
            @AuthenticationPrincipal Object principal,
            @Parameter(description = "İdman zalının ID-si") @PathVariable Long gymId) {
        Long userId = UserContext.extractUserId(principal);
        return ResponseEntity.ok(this.gymReadService.getGymDetailV2(userId, gymId));
    }

    @GetMapping
    @Operation(summary = "İdman zallarını əldə edin (V2)", description = "Çoxlu kateqoriya dəstəyi ilə idman zalları siyahısını qaytarır.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "İdman zalları uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))})
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaginatedResponse<GymMainPageResponseV2>> getGyms(
            @AuthenticationPrincipal Object principal,
            @Parameter(description = "Axtarış sorğusu") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Filtr növü (ALL, NEW, CLOSEST, SAVED)") @RequestParam(value = "type", defaultValue = "ALL") String type,
            @Parameter(description = "Kateqoriya ID-si") @RequestParam(value = "category", required = false) Long categoryId,
            @Parameter(description = "Abunəlik ID-si") @RequestParam(value = "subscriptionId", required = false) Long subscriptionId,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size,
            @Parameter(description = "İstifadəçinin enliyi (latitude)") @RequestParam(value = "lat", required = false) Double lat,
            @Parameter(description = "İstifadəçinin uzunluğu (longitude)") @RequestParam(value = "lng", required = false) Double lng,
            @Parameter(description = "Sıralama istiqaməti. ASC, DESC") @RequestParam(value = "sort_dir", defaultValue = "DESC") SortDirection sortDir) {
        Long userId = UserContext.extractUserId(principal);
        int safePageSize = Math.min(page_size, 100);
        return ResponseEntity.ok(this.gymReadService.getGymsV2(userId, q, type, categoryId, subscriptionId, page, safePageSize, lat, lng, sortDir.name().toLowerCase()));
    }
}
