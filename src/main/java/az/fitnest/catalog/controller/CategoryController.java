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
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.MessageSource;

import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.user.grpc.UserResponse;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Fitnes kataloqu kateqoriyalarına baxmaq üçün ucluqlar")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final MessageSource messageSource;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Operation(summary = "Bütün kateqoriyaları əldə edin", description = "Bütün kataloq kateqoriyalarının səhifələnmiş siyahısını qaytarır. Ada görə axtarış üçün 'q' parametrindən istifadə edin.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Kateqoriyalar uğurla əldə edildi")})
    @GetMapping
    public ResponseEntity<PaginatedResponse<CategoryDto>> getAllCategories(
            @Parameter(description = "Axtarış üçün ad") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Səhifə indeksi (1-dən başlayaraq)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Hər səhifədəki elementlərin sayı") @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            Locale locale) {
        if (page < 1) {
            String message = messageSource.getMessage("error.page_index_invalid", null, locale);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, message);
        }
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<Category> categories;
        if (q != null && !q.isBlank()) {
            categories = this.categoryRepository.searchByName(q, pageable);
        } else {
            categories = this.categoryRepository.findAll(pageable);
        }
        // Determine user language
        String language = "AZ";
        if (userId != null) {
            UserResponse user = userServiceGrpcClient.getUserById(userId);
            if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                language = user.getLanguage();
            }
        }
        final String userLanguage = language;
        List<CategoryDto> items = categories.getContent().stream()
            .map(c -> {
                String localizedName = c.getName();
                if (!"AZ".equalsIgnoreCase(userLanguage)) {
                    String translated = translationService.getTranslatedValue("Category", String.valueOf(c.getId()), "name", userLanguage);
                    if (translated != null && !translated.isEmpty()) {
                        localizedName = translated;
                    }
                }
                return CategoryDto.builder()
                    .id(c.getId())
                    .name(localizedName)
                    .photoUrl(c.getPhotoUrl())
                    .iconUrl(c.getIconUrl())
                    .build();
            })
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
