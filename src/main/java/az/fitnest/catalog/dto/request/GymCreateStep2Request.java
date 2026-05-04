package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import java.util.Set;
import lombok.Builder;

@Builder
public record GymCreateStep2Request(
    Set<GymWorkHourResponse> generalWorkHours,
    Set<GymWorkHourResponse> workHoursWoman,
    Set<GymWorkHourResponse> workHoursMan
) {}
