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

    private final az.fitnest.catalog.repository.LessonTypeRepository lessonTypeRepository;

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

        if (gymRepository.existsByCategoryId(categoryId)) {
            throw new BadRequestException("CATEGORY_IN_USE", "error.category_in_use");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public void deleteAllCategories() {
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
        String fullUrl = fileStorageService.saveFile(validatedFile, "/categories/" + categoryId);
        category.setPhotoUrl(fullUrl);
        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategoryIcon(Long categoryId, MultipartFile file) {
        MultipartFile validatedFile = fileStorageService.validateAndWrapImage(file);
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        String fullUrl = fileStorageService.saveFile(validatedFile, "/categories/icons/" + categoryId);
        category.setIconUrl(fullUrl);
        categoryRepository.save(category);
    }

    private CategoryResponse mapToDto(Category category, String language) {
        String localizedName = translationService.getTranslatedValue("CATEGORY", String.valueOf(category.getId()), "name", language);
        if (localizedName == null || localizedName.isEmpty()) {
            localizedName = category.getName();
        }
        List<LessonTypeResponse> lessonTypeResponses = null;
        if (category.getLessonTypes() != null) {
            lessonTypeResponses = category.getLessonTypes().stream()
                    .map(lt -> {
                        String localizedLt = translationService.getTranslatedValue("LessonType", String.valueOf(lt.getId()), "name", language);
                        return new LessonTypeResponse(lt.getId(), localizedLt != null ? localizedLt : lt.getName());
                    })
                    .collect(Collectors.toList());
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(localizedName)
                .photoUrl(category.getPhotoUrl())
                .iconUrl(category.getIconUrl())
                .lessonTypes(lessonTypeResponses)
                .build();
    }

    private String resolveUserLanguage() {
        Long userId = UserContext.getCurrentUserId();
        return resolveUserLanguage(userId);
    }

    private String resolveUserLanguage(Long userId) {
        // 1. Check current request Accept-Language header first via LocaleContextHolder
        try {
            String localeLang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage()
                    .toUpperCase();
            if (localeLang.equals("EN") || localeLang.equals("RU")) {
                return localeLang;
            }
        } catch (Exception ignored) {
        }

        // 2. Fallback to GRPC User Profile language
        if (userId != null) {
            try {
                UserResponse user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                    return user.getLanguage().toUpperCase();
                }
            } catch (Exception ignored) {
            }
        }
        return "AZ";
    }

    @Transactional
    public CategoryResponse createCategory(String name, MultipartFile photo, List<Long> lessonTypeIds) {
        Category category = Category.builder().name(name).build();
        if (lessonTypeIds != null && !lessonTypeIds.isEmpty()) {
            category.setLessonTypes(new java.util.HashSet<>(lessonTypeRepository.findAllById(lessonTypeIds)));
        }
        category = categoryRepository.save(category);

        translationService.autoTranslateAndSave("CATEGORY", String.valueOf(category.getId()), "name", name);

        if (photo != null && !photo.isEmpty()) {
            MultipartFile validatedPhoto = fileStorageService.validateAndWrapImage(photo);
            category.setPhotoUrl(fileStorageService.saveFile(validatedPhoto, "/categories/" + category.getId()));
            category = categoryRepository.save(category);
        }

        return mapToDto(category, resolveUserLanguage());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, String name, MultipartFile photo, List<Long> lessonTypeIds) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        category.setName(name);

        if (lessonTypeIds != null) {
            category.setLessonTypes(new java.util.HashSet<>(lessonTypeRepository.findAllById(lessonTypeIds)));
        }

        if (photo != null && !photo.isEmpty()) {
            MultipartFile validatedPhoto = fileStorageService.validateAndWrapImage(photo);
            category.setPhotoUrl(fileStorageService.saveFile(validatedPhoto, "/categories/" + category.getId()));
        }

        category = categoryRepository.save(category);

        translationService.autoTranslateAndSave("CATEGORY", String.valueOf(category.getId()), "name", name);

        return mapToDto(category, resolveUserLanguage());
    }
}
