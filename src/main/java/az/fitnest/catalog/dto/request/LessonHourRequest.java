package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record LessonHourRequest(
    @NotNull Long trainerId,
    @NotNull Long lessonTypeId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull Integer maxSlots
) {}
