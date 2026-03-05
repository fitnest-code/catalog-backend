package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymMainPageDto(
    String gymId,
    String name,
    String coverImageUrl,
    double stars,
    boolean isNew,
    String location,
    String city,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    Double distanceKm,
    boolean isSaved
) {}
