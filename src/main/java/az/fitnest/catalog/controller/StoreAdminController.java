package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.AddDiscountRequest;
import az.fitnest.catalog.dto.StoreDetailResponseDto;
import az.fitnest.catalog.dto.StoreRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
@Tag(name = "Store Admin", description = "Mağazaları idarə etmək üçün administrativ ucluqlar. Bu ucluqlar yalnız ADMIN və SUPER_ADMIN rollarına malik istifadəçilər tərəfindən istifadə edilə bilər.")
@SecurityRequirement(name = "bearerAuth")
public class StoreAdminController {

    private final StoreService storeService;
    private final StoreAdminService storeAdminService;

    @Operation(summary = "Yeni mağaza yaradın", description = "Sistemə yeni mağaza əlavə edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<StoreDetailResponseDto> createStore(@Valid @RequestBody StoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request));
    }

    @Operation(summary = "Mağazanı yeniləyin", description = "Mövcud mağazanın məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StoreDetailResponseDto> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
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

    @Operation(summary = "Bütün mağazaları silin (Kritik)", description = "Sistemdəki BÜTÜN mağazaları silir. Bu əməliyyat üçün SUPER_ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
    @PostMapping("/{storeId}/image")
    public ResponseEntity<String> uploadStoreImage(@PathVariable Long storeId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(storeService.uploadStoreImage(storeId, file));
    }
}
