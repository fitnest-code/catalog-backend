package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.CategoryResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.dto.request.CategoryRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CategoryService {
    PaginatedResponse<CategoryResponse> getCategories(String q, int page, int size);
    List<Category> getAllCategoriesLocalized(Long userId);
    void deleteCategory(Long categoryId);
    void deleteAllCategories();
    void updateCategoryName(Long categoryId, String newName);
    void updateCategoryPhoto(Long categoryId, MultipartFile file);
    void updateCategoryIcon(Long categoryId, MultipartFile file);
    CategoryResponse createCategory(String name, MultipartFile photo);
    CategoryResponse updateCategory(Long id, String name, MultipartFile photo);
}
