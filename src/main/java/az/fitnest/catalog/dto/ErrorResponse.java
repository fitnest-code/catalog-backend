package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    OffsetDateTime timestamp,
    Integer status,
    String error,
    String path,
    Map<String, Object> details
) {
    public static class ErrorResponseBuilder {
        private OffsetDateTime timestamp = OffsetDateTime.now();
    }
}
