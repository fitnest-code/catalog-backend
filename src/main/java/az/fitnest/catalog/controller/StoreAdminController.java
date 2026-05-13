package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.request.StoreStep2Request;
import az.fitnest.catalog.dto.request.StoreStep3Request;
import az.fitnest.catalog.dto.request.StoreUpdateRequest;
import az.fitnest.catalog.dto.response.AdminStoreDetailResponse;
import az.fitnest.catalog.service.StoreAdminService;
import az.fitnest.catalog.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
@Tag(name = "Store Admin", description = "Mağazaları idarə etmək üçün administrativ ucluqlar. Bu ucluqlar yalnız ADMIN və SUPER_ADMIN rollarına malik istifadəçilər tərəfindən istifadə edilə bilər.")
@SecurityRequirement(name = "bearerAuth")
public class StoreAdminController {

    private final StoreService storeService;
    private final StoreAdminService storeAdminService;

    @Operation(summary = "Yeni mağaza - Addım 1", description = "Mağazanın adını və üz qabığı şəklini qəbul edir, DRAFT statusu ilə yaradır.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/step1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Long>> createMarketStep1(
            @Parameter(description = "Mağazanın adı", required = true, example = "FitLife Market")
            @RequestParam("name") String name,

            @Parameter(description = "Mağazanın üz qabığı şəkli", required = true)
            @RequestParam("photo") MultipartFile photo) {

        Long storeId = storeAdminService.createMarketStep1(name, photo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Collections.singletonMap("id", storeId));
    }

    @Operation(summary = "Yeni mağaza - Addım 2", description = "Mağazanın məkan, əlaqə və iş saatı məlumatlarını yeniləyir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/step2", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createStoreStep2(
            @PathVariable Long id,
            @Valid @RequestBody StoreStep2Request request) {

        storeAdminService.createMarketStep2(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Yeni mağaza - Addım 3", description = "Mağazanın paket endirim faizlərini təyin edir və statusunu ACTIVE edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/step3", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createStoreStep3(
            @PathVariable Long id,
            @Valid @RequestBody StoreStep3Request request) {

        storeAdminService.createMarketStep3(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mağazanı yenilə", description = "Yalnız göndərilən sahələr yenilənir. " + "Göndərilməyən sahələr dəyişmir. ")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateMarket(
            @Parameter(description = "Mağazanın ID-si", required = true)
            @PathVariable Long id,

            @Parameter(description = "Yenilənəcək sahələr (JSON). Yalnız dəyişdirilmək istənən sahələri göndər.")
            @RequestPart("data") @Valid StoreUpdateRequest request,

            @Parameter(description = "Yeni üz qabığı şəkli (məcburi deyil)")
            @RequestPart(value = "photo", required = false) MultipartFile photo) {

        storeAdminService.updateStore(id, request, photo);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary     = "Mağazanı sil",
            description = "Mağazanı və bütün bağlı şəkilləri storage ilə birlikdə silir. Bu əməliyyat geri alına bilməz."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(
            @Parameter(description = "Silinəcək mağazanın ID-si", required = true)
            @PathVariable Long id) {

        storeAdminService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağaza statusunu yeniləyin", description = "Mağazanın aktiv, passiv və ya digər statuslarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStoreStatus(@PathVariable Long id, @RequestParam String status) {
        storeAdminService.updateStoreStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mağazaların siyahısını alın", description = "Mağazaların adını, ünvanını və telefon nömrəsini qaytarır. Axtarış və sıralama dəstəklənir.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<az.fitnest.catalog.dto.PaginatedResponse<az.fitnest.catalog.dto.response.AdminStoreResponse>> getAllStores(
            @io.swagger.v3.oas.annotations.Parameter(description = "Mağaza adı, ünvan və ya şəhər üzrə axtarış") @RequestParam(required = false) String query,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sıralama qaydası. Dəyərlər: "
                    + "name_asc - Ad : A-Z, "
                    + "name_desc - Ad : Z-A, "
                    + "address_asc - Şəhər + Ünvan (A-Z), "
                    + "newest - Yeni əlavə edilmiş (son 1 həftə)", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                    "name_asc", "name_desc", "address_asc", "newest"
            })) @RequestParam(required = false) String sort,
            @io.swagger.v3.oas.annotations.Parameter(description = "Səhifə nömrəsi (1-dən başlayır)", example = "1") @RequestParam(defaultValue = "1") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Hər səhifədəki elementlərin sayı", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(storeAdminService.getAllStoresAdmin(query, sort, page, pageSize));
    }

    @Operation(
            summary     = "Mağazanı ID ilə gətir",
            description = "ID-yə uyğun mağazanın bütün detallarını qaytarır."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AdminStoreDetailResponse> getStoreById(
            @Parameter(description = "Mağazanın ID-si", required = true)
            @PathVariable Long id) {

        return ResponseEntity.ok(storeAdminService.getStoreById(id));
    }
}
