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

    boolean existsByUserIdAndReservationDateIdAndStatusIn(Long userId, Long reservationDateId, java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    int countByReservationDateId(Long reservationDateId);

    int countByReservationDateIdAndStatusIn(Long reservationDateId, java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    int countByReservationDateIdAndStatus(Long reservationDateId, az.fitnest.catalog.model.enums.ReservationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT count(r) FROM Reservation r WHERE r.reservationDate.id = :sessionId AND r.status IN (:statuses)")
    int countActiveReservations(@org.springframework.data.repository.query.Param("sessionId") Long sessionId, @org.springframework.data.repository.query.Param("statuses") java.util.List<az.fitnest.catalog.model.enums.ReservationStatus> statuses);
}
