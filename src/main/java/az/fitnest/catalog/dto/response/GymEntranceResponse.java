package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GymEntranceResponse(
    @Schema(description = "Whether the user is allowed to enter the gym", example = "true", implementation = Boolean.class)
    boolean allowed
) {}
