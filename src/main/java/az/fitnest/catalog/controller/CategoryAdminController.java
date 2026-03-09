package az.fitnest.catalog.controller;

import az.fitnest.catalog.service.impl.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PutMapping("/{categoryId}/photo")
    public ResponseEntity<Void> updateCategoryPhoto(
            @PathVariable Long categoryId,
            @RequestParam String photoUrl) {
        categoryService.updateCategoryPhoto(categoryId, photoUrl);
        return ResponseEntity.ok().build();
    }
}
