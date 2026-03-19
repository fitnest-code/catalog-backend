package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalTime;

import az.fitnest.catalog.model.enums.GymWorkHourPeriod;

@Builder
public record GymWorkHourDto(
    @Schema(description = "Period", example = "WEEKDAYS") GymWorkHourPeriod period,
    @Schema(description = "Start time", example = "09:00") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime from,
    @Schema(description = "End time", example = "21:00") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime to
) {}
