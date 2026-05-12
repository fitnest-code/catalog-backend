package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.LessonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonTypeRepository extends JpaRepository<LessonType, Long> {
}
