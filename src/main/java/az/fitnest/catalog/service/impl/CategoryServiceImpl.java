package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.response.CategoryResponse;
import az.fitnest.catalog.dto.request.CategoryRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements az.fitnest.catalog.service.CategoryService {

    private final CategoryRepository categoryRepository;
    private final GymRepository gymRepository;
    private final FileStorageService fileStorageService;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Transactional(readOnly = true)
    public PaginatedResponse<CategoryResponse> getCategories(String q, int page, int size) {
        if (page < 1) {
            throw new BadRequestException("PAGE_INVALID", "error.page_index_invalid");
        }
        PageRequest pageable = PageRequest.of(page - 1, size);

        Page<Category> categories;
        if (q != null && !q.isBlank()) {
            categories = categoryRepository.searchByName(q, pageable);
        } else {
            categories = categoryRepository.findAll(pageable);
        }

        String userLanguage = resolveUserLanguage();

        List<CategoryResponse> items = categories.getContent().stream()
                .map(c -> mapToDto(c, userLanguage))
                .collect(Collectors.toList());

        return PaginatedResponse.<CategoryResponse>builder()
                .items(items)
                .total(categories.getTotalElements())
                .page(page)
                .pageSize(size)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesLocalized(Long userId) {
        String language = resolveUserLanguage(userId);
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            String translatedName = translationService.getTranslatedValue("CATEGORY", String.valueOf(category.getId()), "name", language);
            if (translatedName != null && !translatedName.isEmpty()) {
                category.setName(translatedName);
            }
        }
        return categories;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));

        if (!category.getGyms().isEmpty()) {
            throw new BadRequestException("CATEGORY_IN_USE", "error.category_in_use");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public void deleteAllCategories() {
        gymRepository.truncateGymCategories();
        categoryRepository.deleteAll();
    }

    @Transactional
    public void updateCategoryName(Long categoryId, String newName) {
        categoryRepository.findById(categoryId).ifPresent(category -> {
            category.setName(newName);
            categoryRepository.save(category);
        });
    }

    @Transactional
    public void updateCategoryPhoto(Long categoryId, MultipartFile file) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(file);
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        String fsId = fileStorageService.saveFile(validatedFile, "/categories/" + categoryId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setPhotoUrl(fullUrl);
        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategoryIcon(Long categoryId, MultipartFile file) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(file);
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        String fsId = fileStorageService.saveFile(validatedFile, "/categories/icons/" + categoryId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setIconUrl(fullUrl);
        categoryRepository.save(category);
    }

    private CategoryResponse mapToDto(Category category, String language) {
        String localizedName = translationService.getTranslatedValue("CATEGORY", String.valueOf(category.getId()), "name", language);
        if (localizedName == null || localizedName.isEmpty()) {
            localizedName = category.getName();
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(localizedName)
                .photoUrl(category.getPhotoUrl())
                .iconUrl(category.getIconUrl())
                .build();
    }

    private String resolveUserLanguage() {
        Long userId = UserContext.getCurrentUserId();
        return resolveUserLanguage(userId);
    }

    private String resolveUserLanguage(Long userId) {
        if (userId != null) {
            try {
                UserResponse user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                    return user.getLanguage();
                }
            } catch (Exception ignored) {
            }
        }
        return "AZ";
    }

    @Transactional
    public CategoryResponse createCategory(String name, MultipartFile photo) {
        Category category = Category.builder().name(name).build();
        category = categoryRepository.save(category);

        if (photo != null && !photo.isEmpty()) {
            MultipartFile validatedPhoto = fileStorageService.validateAndWrapImage(photo);
            String fsId = fileStorageService.saveFile(validatedPhoto, "/categories/" + category.getId());
            category.setPhotoUrl("/api/v1/media/stream/" + fsId);
            category = categoryRepository.save(category);
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .photoUrl(category.getPhotoUrl())
                .iconUrl(category.getIconUrl())
                .build();
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, String name, MultipartFile photo) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        category.setName(name);

        if (photo != null && !photo.isEmpty()) {
            MultipartFile validatedPhoto = fileStorageService.validateAndWrapImage(photo);
            String fsId = fileStorageService.saveFile(validatedPhoto, "/categories/" + category.getId());
            category.setPhotoUrl("/api/v1/media/stream/" + fsId);
        }

        category = categoryRepository.save(category);

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .photoUrl(category.getPhotoUrl())
                .iconUrl(category.getIconUrl())
                .build();
    }
}
