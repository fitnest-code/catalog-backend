package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class GymWorkHour {
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.ORDINAL)
    private GymWorkHourPeriod period;
    private LocalTime fromTime;
    private LocalTime toTime;
}
