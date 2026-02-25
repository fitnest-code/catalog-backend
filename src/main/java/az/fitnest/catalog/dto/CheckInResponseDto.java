package az.fitnest.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class CheckInResponseDto {
    private String addressText;
    private LocalDate visitDate;
    private LocalTime visitHour;
}
