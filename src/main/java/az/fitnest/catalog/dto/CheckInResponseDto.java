package az.fitnest.catalog.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponseDto {
    private String addressText;
    private LocalDate visitDate;
    private LocalTime visitHour;


}
