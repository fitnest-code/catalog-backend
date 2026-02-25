package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Profession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessionRepository extends JpaRepository<Profession, Long> {
    boolean existsByName(String name);
}
