package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import az.fitnest.catalog.model.entity.Category;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final GymRepository gymRepository;
    private final FileStorageService fileStorageService;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;

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

    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesLocalized(Long userId) {
        String language = "AZ";
        if (userId != null) {
            UserResponse user = userServiceGrpcClient.getUserById(userId);
            if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                language = user.getLanguage();
            }
        }
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            String translatedName = translationService.getTranslatedValue("Category", String.valueOf(category.getId()), "name", language);
            if (translatedName != null && !translatedName.isEmpty()) {
                category.setName(translatedName);
            }
        }
        return categories;
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

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
