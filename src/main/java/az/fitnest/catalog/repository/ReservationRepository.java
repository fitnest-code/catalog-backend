package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);
    
    List<Reservation> findByTrainerIdAndReservationDateId(Long trainerId, Long reservationDateId);
    
    boolean existsByUserIdAndReservationDateId(Long userId, Long reservationDateId);

    int countByReservationDateId(Long reservationDateId);
}
