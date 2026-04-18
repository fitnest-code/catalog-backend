package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.ReservationCancelReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationCancelReasonRepository extends JpaRepository<ReservationCancelReason, Long> {
    List<ReservationCancelReason> findByStatusOrderByCreatedDateAsc(String status);
    Optional<ReservationCancelReason> findByCode(String code);
}
