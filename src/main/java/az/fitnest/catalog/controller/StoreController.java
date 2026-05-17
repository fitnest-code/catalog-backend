package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.ErrorResponse;
import az.fitnest.catalog.dto.response.LocationResponse;
import az.fitnest.catalog.dto.response.StoreDetailResponse;
import az.fitnest.catalog.dto.response.StoreMainPageResponse;
import az.fitnest.catalog.service.StoreService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Kataloq mağazalarına baxmaq və istifadəçilərin mağazalarla qarşılıqlı əlaqəsi üçün ucluqlar.")
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "Kataloq mağazalarını əldə edin", description = "Bütün mağaza siyahıları üçün birləşdirilmiş ucluq (Hamısı, Ən yaxın, Yeni, Endirimlilər, Saxlanılanlar). Görünüşləri dəyişmək üçün 'type' parametrindən istifadə edin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mağazalar uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Autentifikasiya tələb olunur", content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<PaginatedResponse<StoreMainPageResponse>> getStores(
            @Parameter(description = "Axtarış sorğusu (ad və ya ünvana görə)") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Filtr növü (ALL, NEW, DISCOUNTED, CLOSEST, SAVED)") @RequestParam(value = "type", defaultValue = "ALL") String type,
            @Parameter(description = "İstifadəçinin enliyi (CLOSEST növü üçün tələb olunur)") @RequestParam(value = "lat", required = false) Double lat,
            @Parameter(description = "İstifadəçinin uzunluğu (CLOSEST növü üçün tələb olunur)") @RequestParam(value = "lng", required = false) Double lng,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int page_size,
            @Parameter(description = "Çeşidləmə qaydası (asc, desc)") @RequestParam(value = "sort_dir", defaultValue = "desc") String sortDir) {
        Long userId = UserContext.getCurrentUserId();

        return ResponseEntity.ok(this.storeService.getStores(userId, q, type, lat, lng, page, page_size, sortDir));
    }

    @Operation(summary = "Mağaza təfərrüatlarını əldə edin", description = "Xüsusi bir mağazanın tam təfərrüatlarını əldə edir.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mağaza təfərrüatları uğurla əldə edildi", content = {@Content(schema = @Schema(implementation = StoreDetailResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Mağaza tapılmadı", content = {@Content(schema = @Schema(implementation = ErrorResponse.class))})
    })
    @GetMapping("/{storeId:\\d+}")
    public ResponseEntity<StoreDetailResponse> getStoreDetail(@Parameter(description = "Mağazanın ID-si") @PathVariable Long storeId) {
        Long userId = UserContext.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStoreDetail(userId, storeId));
    }

    @Operation(summary = "Mağaza məkanını əldə edin", description = "Xüsusi mağazanın koordinatlarını və ünvan mətnini qaytarır.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mağaza məkanı əldə edildi", content = {@Content(schema = @Schema(implementation = LocationResponse.class))}),
            @ApiResponse(responseCode = "404", description = "Mağaza tapılmadı")
    })
    @GetMapping("/{storeId:\\d+}/location")
    public ResponseEntity<LocationResponse> getStoreLocation(@Parameter(description = "Mağazanın ID-si") @PathVariable Long storeId) {
        return ResponseEntity.ok(this.storeService.getStoreLocation(storeId));
    }

    @Operation(summary = "Mağazanı saxla/sil", description = "Mağazanın 'saxlanılanlar' statusunu dəyişir.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dəyişiklik uğurludur"),
            @ApiResponse(responseCode = "404", description = "Mağaza tapılmadı")
    })
    @PostMapping("/{storeId}/save")
    public ResponseEntity<Map<String, Boolean>> saveStore(@PathVariable Long storeId) {
        Long userId = UserContext.getCurrentUserId();
        boolean isSaved = this.storeService.toggleSave(userId, storeId);
        return ResponseEntity.ok(Map.of("is_saved", isSaved));
    }

}
