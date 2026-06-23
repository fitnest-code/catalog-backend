package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.TrainerReservationDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerReservationDateRepository extends JpaRepository<TrainerReservationDate, Long> {
    List<TrainerReservationDate> findByGymId(Long gymId);

    boolean existsByGymId(Long gymId);

    List<TrainerReservationDate> findByTrainerId(Long trainerId);

    List<TrainerReservationDate> findByTrainerIdOrderByDateAscStartTimeAsc(Long trainerId);

    Optional<TrainerReservationDate> findByTrainerIdAndDateAndStartTimeAndEndTime(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime);

    List<TrainerReservationDate> findByTrainerIdAndDate(Long trainerId, LocalDate date);

    List<TrainerReservationDate> findByGymIdAndClassTypeIdAndDate(Long gymId, Long classTypeId, LocalDate date);

    List<TrainerReservationDate> findByGymIdAndClassTypeIdAndTrainerIdAndDate(Long gymId, Long classTypeId, Long trainerId, LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(trd) > 0 FROM TrainerReservationDate trd WHERE trd.trainer.id = :trainerId AND trd.date = :date " +
            "AND trd.startTime < :endTime AND trd.endTime > :startTime")
    boolean existsOverlappingAvailability(
            @org.springframework.data.repository.query.Param("trainerId") Long trainerId,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date,
            @org.springframework.data.repository.query.Param("startTime") java.time.LocalTime startTime,
            @org.springframework.data.repository.query.Param("endTime") java.time.LocalTime endTime);

    @org.springframework.data.jpa.repository.Query("SELECT trd FROM TrainerReservationDate trd JOIN trd.trainer t WHERE t.gymId = :gymId AND trd.date = :date")
    List<TrainerReservationDate> findByGymIdAndDate(@org.springframework.data.repository.query.Param("gymId") Long gymId, @org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

    List<TrainerReservationDate> findByGymIdAndDateGreaterThanEqual(Long gymId, LocalDate date);

    List<TrainerReservationDate> findByGymIdAndDateBetween(Long gymId, LocalDate startDate, LocalDate endDate);

    List<TrainerReservationDate> findByGymIdAndDateBefore(Long gymId, LocalDate date);
}
