package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final GymRepository gymRepository;

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
    public void updateCategoryPhoto(Long categoryId, String metaFile) {
        // Process the metaFile instead of photoUrl
        categoryRepository.findById(categoryId).ifPresent(category -> {
            // Assuming metaFile contains some metadata to be processed
            category.setPhotoMetadata(metaFile); // Replace with actual processing logic
            categoryRepository.save(category);
        });
    }
}
