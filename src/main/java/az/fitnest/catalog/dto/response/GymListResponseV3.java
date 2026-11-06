package az.fitnest.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymListResponseV3(
        List<GymMainPageResponseV3> items,
        GymListMetaV3 meta,
        String message
) {
}
