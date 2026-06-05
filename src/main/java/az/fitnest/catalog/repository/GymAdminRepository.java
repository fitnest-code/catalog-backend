package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface GymAdminRepository extends JpaRepository<GymAdmin, Long> {
    Optional<GymAdmin> findFirstByGymId(Long gymId);
    List<GymAdmin> findByGymId(Long gymId);
    List<GymAdmin> findAllByGymIdIn(List<Long> gymIds);
    void deleteAllByGymId(Long gymId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByGymIdAndPhoneNumber(Long gymId, String phoneNumber);
    boolean existsByGymIdAndUserId(Long gymId, Long userId);
    java.util.List<GymAdmin> findByUserId(Long userId);
    @Query("SELECT ga FROM GymAdmin ga LEFT JOIN FETCH ga.gym WHERE ga.userId IN :userIds")
    List<GymAdmin> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
