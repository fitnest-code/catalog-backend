package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.CategoryDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Fitnes kataloqu kateqoriyalarına baxmaq üçün ucluqlar")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @Operation(summary = "Bütün kateqoriyaları əldə edin", description = "Bütün kataloq kateqoriyalarının səhifələnmiş siyahısını qaytarır. Ada görə axtarış üçün 'q' parametrindən istifadə edin.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Kateqoriyalar uğurla əldə edildi")})
    @GetMapping
    public ResponseEntity<PaginatedResponse<CategoryDto>> getAllCategories(
            @Parameter(description = "Axtarış üçün ad") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<Category> categories;
        if (q != null && !q.isBlank()) {
            categories = this.categoryRepository.searchByName(q, pageable);
        } else {
            categories = this.categoryRepository.findAll(pageable);
        }
        List<CategoryDto> items = categories.getContent().stream()
            .map(c -> CategoryDto.builder().id(c.getId()).name(c.getName()).photoUrl(c.getPhotoUrl()).build())
            .collect(Collectors.toList());
        PaginatedResponse<CategoryDto> resp = PaginatedResponse.<CategoryDto>builder()
            .items(items)
            .total(categories.getTotalElements())
            .page(page)
            .pageSize(size)
            .build();
        return ResponseEntity.ok(resp);
    }
}
