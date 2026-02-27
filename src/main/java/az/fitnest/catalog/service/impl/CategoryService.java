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
        // Clear all gym-category associations first to avoid FK violations
        gymRepository.truncateGymCategories();
        // Now it's safe to delete all categories
        categoryRepository.deleteAll();
    }
}
