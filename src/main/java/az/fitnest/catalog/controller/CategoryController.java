/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  io.swagger.v3.oas.annotations.Parameter
 *  io.swagger.v3.oas.annotations.responses.ApiResponse
 *  io.swagger.v3.oas.annotations.responses.ApiResponses
 *  io.swagger.v3.oas.annotations.security.SecurityRequirement
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.access.prepost.PreAuthorize
 *  org.springframework.validation.BeanPropertyBindingResult
 *  org.springframework.validation.BindingResult
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.CategoryDto;
import az.fitnest.catalog.dto.CategoryRequest;
import az.fitnest.catalog.dto.PaginatedResponse;
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
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value={"/api/v1"})
@Tag(name="Categories", description="Endpoints for managing fitness catalog categories (e.g., Supplements, Apparel, Gyms)")
public class CategoryController {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @Operation(summary="Get all categories", description="Returns a paginated list of all catalog categories.")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Categories retrieved successfully")})
    @GetMapping(value={"/categories"})
    public ResponseEntity<PaginatedResponse<CategoryDto>> getAllCategories(@Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int size) {
        PageRequest pageable = PageRequest.of((int)Math.max(0, page - 1), (int)size);
        Page<Category> categories = this.categoryRepository.findAll((Pageable)pageable);
        List<CategoryDto> items = categories.getContent().stream().map(c -> CategoryDto.builder().id(c.getId()).name(c.getName()).photoUrl(c.getPhotoUrl()).build()).collect(Collectors.toList());
        PaginatedResponse<CategoryDto> resp = PaginatedResponse.<CategoryDto>builder().items(items).total(categories.getTotalElements()).page(page).pageSize(size).build();
        return ResponseEntity.ok(resp);
    }

    @Operation(summary="Create category (Admin)", description="Creates a new category for products or services. Requires ADMIN role.")
    @PreAuthorize(value="hasRole('ADMIN')")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="201", description="Category created successfully"), @ApiResponse(responseCode="400", description="Invalid category name or duplicate")})
    @PostMapping(value={"/admin/categories"})
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryRequest request) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "categoryRequest");
        if (request.getName() == null || request.getName().isBlank()) {
            bindingResult.rejectValue("name", "NotBlank", "Category name must not be blank");
        }
        if (this.categoryRepository.existsByName(request.getName())) {
            bindingResult.rejectValue("name", "Duplicate", "Category with this name already exists");
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Validation failed", (BindingResult)bindingResult);
        }
        Category category = new Category();
        category.setName(request.getName());
        Category saved = this.categoryRepository.save(category);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.status(201).body(dto);
    }

    @Operation(summary="Update category (Admin)", description="Updates an existing category. Requires ADMIN role.")
    @PreAuthorize(value="hasRole('ADMIN')")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Category updated successfully"), @ApiResponse(responseCode="404", description="Category not found")})
    @PutMapping(value={"/admin/categories/{id}"})
    public ResponseEntity<CategoryDto> updateCategory(@Parameter(description="ID of the category") @PathVariable Long id, @RequestBody CategoryRequest request) {
        Category existing = (Category)this.categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "categoryRequest");
        if (request.getName() == null || request.getName().isBlank()) {
            bindingResult.rejectValue("name", "NotBlank", "Category name must not be blank");
        }
        if (!existing.getName().equals(request.getName()) && this.categoryRepository.existsByName(request.getName())) {
            bindingResult.rejectValue("name", "Duplicate", "Category with this name already exists");
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Validation failed", (BindingResult)bindingResult);
        }
        existing.setName(request.getName());
        Category saved = this.categoryRepository.save(existing);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary="Delete category (Admin)", description="Deletes a category and its associated photo. Requires ADMIN role.")
    @PreAuthorize(value="hasRole('ADMIN')")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="204", description="Category deleted successfully"), @ApiResponse(responseCode="404", description="Category not found")})
    @DeleteMapping(value={"/admin/categories/{id}"})
    public ResponseEntity<Void> deleteCategory(@Parameter(description="ID of the category to delete") @PathVariable Long id) {
        Category category = (Category)this.categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        if (category.getPhotoUrl() != null && !category.getPhotoUrl().isBlank()) {
            this.fileStorageService.deleteFile(category.getPhotoUrl());
        }
        this.categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary="Upload category image (Admin)", description="Uploads a new image for a category, replacing the old one. Requires ADMIN role.")
    @PreAuthorize(value="hasRole('ADMIN')")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Image uploaded successfully"), @ApiResponse(responseCode="404", description="Category not found")})
    @PutMapping(value={"/admin/categories/{id}/image"}, consumes={"multipart/form-data"})
    public ResponseEntity<CategoryDto> uploadCategoryImage(@Parameter(description="ID of the category") @PathVariable Long id, @Parameter(description="Image file to upload") @RequestParam(value="file") MultipartFile file) {
        Category category = (Category)this.categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        String fsId = this.fileStorageService.saveFile(file, "/categories", category.getPhotoUrl());
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setPhotoUrl(fullUrl);
        Category saved = this.categoryRepository.save(category);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.ok(dto);
    }
}

