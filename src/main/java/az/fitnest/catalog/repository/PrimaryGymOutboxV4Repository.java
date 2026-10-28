package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.PrimaryGymOutboxV4;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface PrimaryGymOutboxV4Repository extends JpaRepository<PrimaryGymOutboxV4, String> {
  @Query("select e from PrimaryGymOutboxV4 e where e.published=false order by e.id")
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  List<PrimaryGymOutboxV4> pending(Pageable page);
}
