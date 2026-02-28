package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.StoreDetailResponseDto;
import az.fitnest.catalog.dto.StoreRequest;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
@Tag(name = "Store Admin", description = "Mağazaları idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class StoreAdminController {

    private final StoreService storeService;

    @Operation(summary = "Mağaza yaradın (Admin)", description = "Yeni mağaza yaradır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Mağaza uğurla yaradıldı"), @ApiResponse(responseCode = "400", description = "Yanlış mağaza məlumatı")})
    @PostMapping
    public ResponseEntity<StoreDetailResponseDto> createStoreAdmin(@RequestBody StoreRequest request) {
        return ResponseEntity.status(201).body(this.storeService.createStore(request));
    }

    @Operation(summary = "Mağazanı yeniləyin (Admin)", description = "Mövcud mağazanı yeniləyir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Mağaza uğurla yeniləndi"), @ApiResponse(responseCode = "404", description = "Mağaza tapılmadı")})
    @PutMapping("/{storeId}")
    public ResponseEntity<StoreDetailResponseDto> updateStoreAdmin(@PathVariable Long storeId, @RequestBody StoreRequest request) {
        return ResponseEntity.ok(this.storeService.updateStore(storeId, request));
    }

    @Operation(summary = "Mağazanı silin (Admin)", description = "Mağazanı həmişəlik silir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Mağaza uğurla silindi")})
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStoreAdmin(@PathVariable Long storeId) {
        this.storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağaza loqosunu yeniləyin (Admin)", description = "Mağazanın loqo şəklini yükləyir və ya əvəz edir. ADMIN rolu tələb olunur.")
    @PutMapping(value = "/{storeId}/logo", consumes = {"multipart/form-data"})
    public ResponseEntity<Void> updateStoreLogo(@PathVariable Long storeId, @RequestParam(value = "file") MultipartFile file) {
        Store store = this.storeService.getStoreEntityById(storeId);
        if (store.getLogoUrl() != null && !store.getLogoUrl().isBlank()) {
            this.storeService.deleteFileSafely(store.getLogoUrl());
        }
        String fullUrl = this.storeService.uploadFileDirectly(storeId, file);
        this.storeService.updateStoreLogoUrl(storeId, fullUrl);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağaza loqosunu silin (Admin)", description = "Mağazanın loqo şəklini silir. ADMIN rolu tələb olunur.")
    @DeleteMapping("/{storeId}/logo")
    public ResponseEntity<Void> deleteStoreLogo(@PathVariable Long storeId) {
        Store store = this.storeService.getStoreEntityById(storeId);
        if (store.getLogoUrl() != null && !store.getLogoUrl().isBlank()) {
            this.storeService.deleteFileSafely(store.getLogoUrl());
            this.storeService.updateStoreLogoUrl(storeId, null);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağazanın üz qabığı şəklini yeniləyin (Admin)", description = "Mağazanın üz qabığı şəklini yükləyir və ya əvəz edir. ADMIN rolu tələb olunur.")
    @PutMapping(value = "/{storeId}/cover", consumes = {"multipart/form-data"})
    public ResponseEntity<Void> updateStoreCover(@PathVariable Long storeId, @RequestParam(value = "file") MultipartFile file) {
        Store store = this.storeService.getStoreEntityById(storeId);
        if (store.getCoverImageUrl() != null && !store.getCoverImageUrl().isBlank()) {
            this.storeService.deleteFileSafely(store.getCoverImageUrl());
        }
        String fullUrl = this.storeService.uploadFileDirectly(storeId, file);
        this.storeService.updateStoreCoverImageUrl(storeId, fullUrl);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mağazanın üz qabığı şəklini silin (Admin)", description = "Mağazanın üz qabığı şəklini silir. ADMIN rolu tələb olunur.")
    @DeleteMapping("/{storeId}/cover")
    public ResponseEntity<Void> deleteStoreCover(@PathVariable Long storeId) {
        Store store = this.storeService.getStoreEntityById(storeId);
        if (store.getCoverImageUrl() != null && !store.getCoverImageUrl().isBlank()) {
            this.storeService.deleteFileSafely(store.getCoverImageUrl());
            this.storeService.updateStoreCoverImageUrl(storeId, null);
        }
        return ResponseEntity.noContent().build();
    }
}
