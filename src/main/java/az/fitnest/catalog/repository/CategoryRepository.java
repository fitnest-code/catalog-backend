package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {
    public boolean existsByName(String var1);
}
