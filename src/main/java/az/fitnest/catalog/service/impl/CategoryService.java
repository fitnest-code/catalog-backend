package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.CategoryDto;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final GymRepository gymRepository;
    private final FileStorageService fileStorageService;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Transactional(readOnly = true)
    public PaginatedResponse<CategoryDto> getCategories(String q, int page, int size) {
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

        List<CategoryDto> items = categories.getContent().stream()
                .map(c -> mapToDto(c, userLanguage))
                .collect(Collectors.toList());

        return PaginatedResponse.<CategoryDto>builder()
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
            String gymNames = category.getGyms().stream()
                    .map(gym -> gym.getName() + " (ID: " + gym.getId() + ")")
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("CATEGORY_IN_USE",
                    "Cannot delete category '" + category.getName() + "' because it is assigned to: " + gymNames);
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
        validateImageFile(file);
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        String fsId = fileStorageService.saveFile(file, "/categories/" + categoryId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setPhotoUrl(fullUrl);
        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategoryIcon(Long categoryId, MultipartFile file) {
        validateImageFile(file);
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "error.category_not_found"));
        String fsId = fileStorageService.saveFile(file, "/categories/icons/" + categoryId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        category.setIconUrl(fullUrl);
        categoryRepository.save(category);
    }

    private CategoryDto mapToDto(Category category, String language) {
        String localizedName = translationService.getTranslatedValue("CATEGORY", String.valueOf(category.getId()), "name", language);
        if (localizedName == null || localizedName.isEmpty()) {
            localizedName = category.getName();
        }
        return CategoryDto.builder()
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

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("FILE_EMPTY", "error.file_empty");
        }
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BadRequestException("FILE_TOO_LARGE", "error.file_too_large");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new BadRequestException("INVALID_FILE_TYPE", "error.invalid_file_type");
        }
    }
}
