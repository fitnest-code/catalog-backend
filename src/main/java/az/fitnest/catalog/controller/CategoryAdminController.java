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
@Tag(name = "Categories Admin", description = "Fitnes kataloqu kateqoriyalarını idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryAdminController {

    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Kateqoriya yaradın (Admin)", description = "Məhsullar və ya xidmətlər üçün yeni kateqoriya yaradır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Kateqoriya uğurla yaradıldı"),
            @ApiResponse(responseCode = "400", description = "Yanlış kateqoriya adı və ya dublikat")
    })
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryRequest request) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "categoryRequest");
        if (request.name() == null || request.name().isBlank()) {
            bindingResult.rejectValue("name", "NotBlank", "Category name must not be blank");
        }
        if (categoryRepository.existsByName(request.name())) {
            bindingResult.rejectValue("name", "Duplicate", "Category with this name already exists");
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Validation failed", (BindingResult) bindingResult);
        }
        Category category = new Category();
        category.setName(request.name());
        Category saved = categoryRepository.save(category);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.status(201).body(dto);
    }

    @Operation(summary = "Kateqoriyanı yeniləyin (Admin)", description = "Mövcud kateqoriyanı yeniləyir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kateqoriya uğurla yeniləndi"),
            @ApiResponse(responseCode = "404", description = "Kateqoriya tapılmadı")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@Parameter(description = "Kateqoriyanın ID-si") @PathVariable Long id, @RequestBody CategoryRequest request) {
        Category existing = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "categoryRequest");
        if (request.name() == null || request.name().isBlank()) {
            bindingResult.rejectValue("name", "NotBlank", "Category name must not be blank");
        }
        if (!existing.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            bindingResult.rejectValue("name", "Duplicate", "Category with this name already exists");
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Validation failed", (BindingResult) bindingResult);
        }
        existing.setName(request.name());
        Category saved = categoryRepository.save(existing);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Kateqoriyanı silin (Admin)", description = "Kateqoriyanı və onunla əlaqəli şəkli silir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Kateqoriya uğurla silindi"),
            @ApiResponse(responseCode = "404", description = "Kateqoriya tapılmadı")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@Parameter(description = "Silinəcək kateqoriyanın ID-si") @PathVariable Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        if (category.getPhotoUrl() != null && !category.getPhotoUrl().isBlank()) {
            fileStorageService.deleteFile(category.getPhotoUrl());
        }
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Kateqoriya şəkli yükləyin (Admin)", description = "Kateqoriya üçün yeni şəkil yükləyir və köhnəsini əvəz edir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Şəkil uğurla yükləndi"),
            @ApiResponse(responseCode = "404", description = "Kateqoriya tapılmadı")
    })
    @PutMapping(value = "/{id}/image", consumes = {"multipart/form-data"})
    public ResponseEntity<CategoryDto> uploadCategoryImage(@Parameter(description = "Kateqoriyanın ID-si") @PathVariable Long id, @Parameter(description = "Yüklənəcək şəkil faylı") @RequestParam(value = "file") MultipartFile file) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        String fsId = fileStorageService.saveFile(file, "/categories", category.getPhotoUrl());
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setPhotoUrl(fullUrl);
        Category saved = categoryRepository.save(category);
        CategoryDto dto = CategoryDto.builder().id(saved.getId()).name(saved.getName()).photoUrl(saved.getPhotoUrl()).build();
        return ResponseEntity.ok(dto);
    }
}
