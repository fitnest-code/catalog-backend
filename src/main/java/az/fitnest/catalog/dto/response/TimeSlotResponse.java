package az.fitnest.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponse {
    @Schema(example = "09:00", type = "string")
    private LocalTime startTime;

    @Schema(example = "10:00", type = "string")
    private LocalTime endTime;

    private Integer emptySpaces;

    private Long sessionId;

    @JsonProperty("isRegisterAcceptable")
    private Boolean registerAcceptable;
}
