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

    Page<Reservation> findByUserIdAndStatus(Long userId, az.fitnest.catalog.model.enums.ReservationStatus status, Pageable pageable);

    List<Reservation> findByTrainerIdAndReservationDateId(Long trainerId, Long reservationDateId);

    boolean existsByUserIdAndReservationDateId(Long userId, Long reservationDateId);

    boolean existsByUserIdAndClassTypeIdAndReservationDateDateAndStatusIn(Long userId, Long classTypeId, java.time.LocalDate date, java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.userId = :userId AND r.reservationDate.date = :date " +
            "AND r.reservationDate.startTime < :endTime AND r.reservationDate.endTime > :startTime " +
            "AND r.status IN :statuses")
    boolean existsOverlappingReservation(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("startTime") java.time.LocalTime startTime,
            @org.springframework.data.repository.query.Param("endTime") java.time.LocalTime endTime,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    int countByReservationDateId(Long reservationDateId);

    int countByReservationDateIdAndStatusIn(Long reservationDateId, java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    int countByReservationDateIdAndStatus(Long reservationDateId, az.fitnest.catalog.model.enums.ReservationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT count(r) FROM Reservation r WHERE r.reservationDate.id = :sessionId AND r.status IN (:statuses)")
    int countActiveReservations(@org.springframework.data.repository.query.Param("sessionId") Long sessionId, @org.springframework.data.repository.query.Param("statuses") java.util.List<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    java.util.Optional<Reservation> findByUserIdAndReservationDateIdAndStatusIn(Long userId, Long reservationDateId, java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);
}
