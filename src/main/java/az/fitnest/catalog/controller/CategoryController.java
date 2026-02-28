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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Fitnes kataloqu kateqoriyalarına baxmaq üçün ucluqlar")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @Operation(summary = "Bütün kateqoriyaları əldə edin", description = "Bütün kataloq kateqoriyalarının səhifələnmiş siyahısını qaytarır.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Kateqoriyalar uğurla əldə edildi")})
    @GetMapping
    public ResponseEntity<PaginatedResponse<CategoryDto>> getAllCategories(@Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<Category> categories = this.categoryRepository.findAll(pageable);
        List<CategoryDto> items = categories.getContent().stream().map(c -> CategoryDto.builder().id(c.getId()).name(c.getName()).photoUrl(c.getPhotoUrl()).build()).collect(Collectors.toList());
        PaginatedResponse<CategoryDto> resp = PaginatedResponse.<CategoryDto>builder().items(items).total(categories.getTotalElements()).page(page).pageSize(size).build();
        return ResponseEntity.ok(resp);
    }
}

