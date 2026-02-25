package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.Map;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @lombok.Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();
    private Integer status;
    private String error;
    private String path;
    private Map<String, Object> details;



    public ErrorResponse(Integer status, String error, String path, Map<String, Object> details) {
        this.status = status;
        this.error = error;
        this.path = path;
        this.details = details;
    }


}
