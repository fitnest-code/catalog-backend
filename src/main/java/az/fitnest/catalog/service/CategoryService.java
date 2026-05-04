package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.CategoryDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.model.entity.Category;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CategoryService {
    PaginatedResponse<CategoryDto> getCategories(String q, int page, int size);
    List<Category> getAllCategoriesLocalized(Long userId);
    void deleteCategory(Long categoryId);
    void deleteAllCategories();
    void updateCategoryName(Long categoryId, String newName);
    void updateCategoryPhoto(Long categoryId, MultipartFile file);
    void updateCategoryIcon(Long categoryId, MultipartFile file);
}
