package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {
    public boolean existsByName(String var1);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Category> searchByName(@Param("q") String q, Pageable pageable);
}
