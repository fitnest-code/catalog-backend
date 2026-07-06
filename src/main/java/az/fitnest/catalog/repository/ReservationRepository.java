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

    @org.springframework.data.jpa.repository.Query(value = "SELECT r FROM Reservation r JOIN FETCH r.reservationDate JOIN FETCH r.gym LEFT JOIN FETCH r.trainer LEFT JOIN FETCH r.classType LEFT JOIN FETCH r.category WHERE r.userId = :userId AND (r.reservationDate.date > CURRENT_DATE OR (r.reservationDate.date = CURRENT_DATE AND r.reservationDate.startTime >= CURRENT_TIME))",
        countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND (r.reservationDate.date > CURRENT_DATE OR (r.reservationDate.date = CURRENT_DATE AND r.reservationDate.startTime >= CURRENT_TIME))")
    Page<Reservation> findUpcomingByUserId(@org.springframework.data.repository.query.Param("userId") Long userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatus(Long userId, az.fitnest.catalog.model.enums.ReservationStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT r FROM Reservation r JOIN FETCH r.reservationDate JOIN FETCH r.gym LEFT JOIN FETCH r.trainer LEFT JOIN FETCH r.classType LEFT JOIN FETCH r.category WHERE r.userId = :userId AND r.status = :status AND (r.reservationDate.date > CURRENT_DATE OR (r.reservationDate.date = CURRENT_DATE AND r.reservationDate.startTime >= CURRENT_TIME))",
        countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.status = :status AND (r.reservationDate.date > CURRENT_DATE OR (r.reservationDate.date = CURRENT_DATE AND r.reservationDate.startTime >= CURRENT_TIME))")
    Page<Reservation> findUpcomingByUserIdAndStatus(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("status") az.fitnest.catalog.model.enums.ReservationStatus status, Pageable pageable);

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

    Page<Reservation> findByGymIdAndStatus(Long gymId, az.fitnest.catalog.model.enums.ReservationStatus status, Pageable pageable);

    Page<Reservation> findByGymId(Long gymId, Pageable pageable);

    long countByGymId(Long gymId);

    long countByGymIdAndStatus(Long gymId, az.fitnest.catalog.model.enums.ReservationStatus status);

    java.util.Optional<Reservation> findFirstByUserIdAndReservationDateIdAndStatusIn(Long userId, Long reservationDateId, java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    boolean existsByGymId(Long gymId);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r WHERE r.userId = :userId AND r.gym.id = :gymId " +
            "AND r.status IN :statuses AND r.reservationDate.date = :date " +
            "AND r.reservationDate.endTime >= :time AND (r.attended = false OR r.attended IS NULL) " +
            "ORDER BY r.reservationDate.startTime ASC")
    List<Reservation> findActiveReservationsForCheckIn(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("gymId") Long gymId,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("time") java.time.LocalTime time,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r WHERE r.status IN :statuses " +
            "AND (r.reservationDate.date < :date OR (r.reservationDate.date = :date AND r.reservationDate.endTime < :time)) " +
            "AND (r.attended = false OR r.attended IS NULL)")
    List<Reservation> findExpiredUnattendedReservations(
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("time") java.time.LocalTime time,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    Page<Reservation> findByStatus(az.fitnest.catalog.model.enums.ReservationStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT r.reservationDate.id, COUNT(r) FROM Reservation r WHERE r.reservationDate.id IN :sessionIds AND r.status IN :statuses GROUP BY r.reservationDate.id")
    List<Object[]> countActiveReservationsForSessions(
            @org.springframework.data.repository.query.Param("sessionIds") java.util.Collection<Long> sessionIds,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reservation r JOIN FETCH r.reservationDate WHERE r.userId = :userId AND r.status IN :statuses AND r.reservationDate.date IN :dates")
    List<Reservation> findReservationsForOverlapCheck(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("dates") java.util.Collection<java.time.LocalDate> dates,
            @org.springframework.data.repository.query.Param("statuses") java.util.Collection<az.fitnest.catalog.model.enums.ReservationStatus> statuses);

    void deleteByUserId(Long userId);
}
