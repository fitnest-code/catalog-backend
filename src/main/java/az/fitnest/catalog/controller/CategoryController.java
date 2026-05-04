package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.response.CategoryResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Fitnes kataloqu kateqoriyalarına baxmaq üçün ucluqlar")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Bütün kateqoriyaları əldə edin", description = "Bütün kataloq kateqoriyalarının səhifələnmiş siyahısını qaytarır. Ada görə axtarış üçün 'q' parametrindən istifadə edin.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Kateqoriyalar uğurla əldə edildi")})
    @GetMapping
    public ResponseEntity<PaginatedResponse<CategoryResponse>> getAllCategories(
            @Parameter(description = "Axtarış üçün ad") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(categoryService.getCategories(q, page, size));
    }
}
