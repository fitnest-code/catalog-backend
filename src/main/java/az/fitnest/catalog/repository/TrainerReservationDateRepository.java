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

    List<TrainerReservationDate> findByTrainerId(Long trainerId);
    
    Optional<TrainerReservationDate> findByTrainerIdAndDateAndStartTimeAndEndTime(Long trainerId, LocalDate date, LocalTime startTime, LocalTime endTime);
    
    List<TrainerReservationDate> findByTrainerIdAndDate(Long trainerId, LocalDate date);
}
