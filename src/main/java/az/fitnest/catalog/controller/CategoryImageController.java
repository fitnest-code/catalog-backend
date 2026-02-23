package az.fitnest.catalog.controller;

import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "Category Images", description = "Endpoints for managing category images")
public class CategoryImageController {

    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    public CategoryImageController(CategoryRepository categoryRepository, FileStorageService fileStorageService) {
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
    }

    @PutMapping(value = "/{id}/image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload category image", description = "Uploads or replaces the image for a category.")
    public ResponseEntity<Category> uploadCategoryImage(
            @Parameter(description = "ID of the category") @PathVariable Long id,
            @Parameter(description = "Image file") @RequestParam("file") MultipartFile file) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        String fsId = fileStorageService.saveFile(file, "/categories", category.getPhotoUrl());
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setPhotoUrl(fullUrl);
        return ResponseEntity.ok(categoryRepository.save(category));
    }
}
