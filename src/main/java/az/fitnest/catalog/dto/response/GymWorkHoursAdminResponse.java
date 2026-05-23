package az.fitnest.catalog.dto.response;

import java.util.Set;
import lombok.Builder;
import az.fitnest.catalog.dto.request.RestDayRequest;

@Builder
public record GymWorkHoursAdminResponse(
    Set<GymWorkHourResponse> generalWorkHours,
    Set<GymWorkHourResponse> workHoursWoman,
    Set<GymWorkHourResponse> workHoursMan,
    Set<RestDayRequest> restDays
) {}
