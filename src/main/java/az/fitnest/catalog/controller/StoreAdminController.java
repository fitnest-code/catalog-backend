package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.request.AddDiscountRequest;
import az.fitnest.catalog.dto.request.StoreRequest;
import az.fitnest.catalog.dto.request.StoreStep2Request;
import az.fitnest.catalog.dto.request.StoreStep3Request;
import az.fitnest.catalog.dto.response.StoreDetailResponse;
import az.fitnest.catalog.service.StoreAdminService;
import az.fitnest.catalog.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<Map<String, Long>> createStoreStep1(
            @RequestParam("name")  String name,
            @RequestParam("photo") MultipartFile photo) {

        Long storeId = storeAdminService.createMarketStep1(name, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", storeId));
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

    @Operation(summary = "Mağazanı yeniləyin", description = "Mövcud mağazanın məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StoreDetailResponse> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return ResponseEntity.ok(storeService.updateStore(id, request));
    }

    @Operation(summary = "Mağazanı silin", description = "Mağazanı sistemdən silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağaza loqosunu yeniləyin", description = "Mağaza üçün loqo şəklini yükləyir və ya yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = storeService.uploadFileDirectly(id, file);
        storeService.updateStoreLogoUrl(id, url);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mağaza üz qabığı şəklini yeniləyin", description = "Mağaza üçün əsas profil şəklini yükləyir və ya yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCoverImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = storeService.uploadFileDirectly(id, file);
        storeService.updateStoreCoverImageUrl(id, url);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün mağazaları silin (Kritik)", description = "Sistemdəki BÜTÜN mağazaları silir. Bu əməliyyat üçün ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllStores() {
        storeService.deleteAllStores();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağaza üçün endirim əlavə edin", description = "Mağazaya xüsusi bir paket üçün endirim faizi əlavə edir. Paket ID-si gRPC vasitəsilə yoxlanılır.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/discounts")
    public ResponseEntity<Void> addDiscount(@PathVariable Long id, @Valid @RequestBody AddDiscountRequest request) {
        storeService.addDiscount(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mağaza statusunu yeniləyin", description = "Mağazanın aktiv, passiv və ya digər statuslarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStoreStatus(@PathVariable Long id, @RequestParam String status) {
        storeAdminService.updateStoreStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{storeId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadStoreImage(@PathVariable Long storeId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(storeService.uploadStoreImage(storeId, file));
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
}
