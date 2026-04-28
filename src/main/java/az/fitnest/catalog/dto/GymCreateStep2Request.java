package az.fitnest.catalog.dto;

import java.util.Set;
import lombok.Builder;

@Builder
public record GymCreateStep2Request(
    Set<GymWorkHourDto> generalWorkHours,
    Set<GymWorkHourDto> workHoursWoman,
    Set<GymWorkHourDto> workHoursMan
) {}
