package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.response.CategoryResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.request.CategoryRequest;
import az.fitnest.catalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Category Admin", description = "Kateqoriyaları idarə etmək üçün administrativ ucluqlar.")
@SecurityRequirement(name = "bearerAuth")
public class CategoryAdminController {
    private final CategoryService categoryService;

    @Operation(summary = "Bütün kateqoriyaları silin (Kritik)", description = "Sistemdəki BÜTÜN kateqoriyaları və onlarla bağlı idman zalı əlaqələrini silir. Bu əməliyyat üçün SUPER_ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllCategories() {
        categoryService.deleteAllCategories();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Kateqoriya adını dəyiş", description = "Kateqoriyanın adını yeniləyir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{categoryId}/name")
    public ResponseEntity<Void> updateCategoryName(
            @PathVariable Long categoryId,
            @RequestParam String newName) {
        categoryService.updateCategoryName(categoryId, newName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Kateqoriya şəklini dəyiş", description = "Kateqoriyanın şəklini yeniləyir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{categoryId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCategoryPhoto(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file) {
        categoryService.updateCategoryPhoto(categoryId, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Kateqoriya ikonunu dəyiş", description = "Kateqoriyanın ikonunu yeniləyir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{categoryId}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCategoryIcon(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file) {
        categoryService.updateCategoryIcon(categoryId, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Kateqoriya yaradın", description = "Yeni bir kateqoriya yaradır.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Kateqoriya uğurla yaradıldı")})
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse categoryDto = categoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + categoryDto.id())).body(categoryDto);
    }

    @Operation(summary = "Kateqoriyanı yeniləyin", description = "Mövcud bir kateqoriyanı yeniləyir.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Kateqoriya uğurla yeniləndi")})
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        CategoryResponse categoryDto = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(categoryDto);
    }

    @Operation(summary = "Kateqoriyanı silin", description = "Mövcud bir kateqoriyanı silir.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Kateqoriya uğurla silindi")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
