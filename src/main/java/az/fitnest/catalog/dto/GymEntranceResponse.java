package az.fitnest.catalog.dto;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymEntranceResponse(
    boolean allowed,
    String gymName,
    String gymLocation,
    String entranceDate,
    String entranceHour,
    Integer visitLimitRemaining,
    az.fitnest.catalog.dto.ApiError error
) {}
