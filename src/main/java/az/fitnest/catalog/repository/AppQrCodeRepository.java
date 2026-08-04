package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.AppQrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppQrCodeRepository extends JpaRepository<AppQrCode, Long> {

    Optional<AppQrCode> findByMode(String mode);

    @Modifying
    @Query("UPDATE AppQrCode q SET q.scanCount = q.scanCount + 1, q.lastModifiedDate = CURRENT_TIMESTAMP WHERE q.mode = :mode")
    int incrementScanCount(@Param("mode") String mode);
}
