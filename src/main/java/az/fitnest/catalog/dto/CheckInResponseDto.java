package az.fitnest.catalog.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponseDto {
    private String addressText;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate visitDate;
    private LocalTime visitHour;

}
