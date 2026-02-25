package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.CategoryDto;
import az.fitnest.catalog.dto.CategoryRequest;
import az.fitnest.catalog.exception.ValidationException;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Categories Admin", description = "Administrative endpoints for managing fitness catalog categories")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryAdminController {

    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Create category (Admin)", description = "Creates a new category for products or services. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid category name or duplicate")
    })
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryRequest request) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "categoryRequest");
        if (request.getName() == null || request.getName().isBlank()) {
            bindingResult.rejectValue("name", "NotBlank", "Category name must not be blank");
        }
        if (categoryRepository.existsByName(request.getName())) {
            bindingResult.rejectValue("name", "Duplicate", "Category with this name already exists");
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Validation failed", (BindingResult) bindingResult);
        }
        Category category = new Category();
        category.setName(request.getName());
        Category saved = categoryRepository.save(category);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.status(201).body(dto);
    }

    @Operation(summary = "Update category (Admin)", description = "Updates an existing category. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@Parameter(description = "ID of the category") @PathVariable Long id, @RequestBody CategoryRequest request) {
        Category existing = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "categoryRequest");
        if (request.getName() == null || request.getName().isBlank()) {
            bindingResult.rejectValue("name", "NotBlank", "Category name must not be blank");
        }
        if (!existing.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            bindingResult.rejectValue("name", "Duplicate", "Category with this name already exists");
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Validation failed", (BindingResult) bindingResult);
        }
        existing.setName(request.getName());
        Category saved = categoryRepository.save(existing);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Delete category (Admin)", description = "Deletes a category and its associated photo. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@Parameter(description = "ID of the category to delete") @PathVariable Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        if (category.getPhotoUrl() != null && !category.getPhotoUrl().isBlank()) {
            fileStorageService.deleteFile(category.getPhotoUrl());
        }
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload category image (Admin)", description = "Uploads a new image for a category, replacing the old one. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PutMapping(value = "/{id}/image", consumes = {"multipart/form-data"})
    public ResponseEntity<CategoryDto> uploadCategoryImage(@Parameter(description = "ID of the category") @PathVariable Long id, @Parameter(description = "Image file to upload") @RequestParam(value = "file") MultipartFile file) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        String fsId = fileStorageService.saveFile(file, "/categories", category.getPhotoUrl());
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setPhotoUrl(fullUrl);
        Category saved = categoryRepository.save(category);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.ok(dto);
    }
}
