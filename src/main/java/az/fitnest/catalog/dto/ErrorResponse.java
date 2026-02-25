package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {

    public ErrorResponse() {}

    public ErrorResponse(ErrorDetail error) {
        this.error = error;
    }

    private ErrorDetail error;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private Integer status;
        private String path;
        private OffsetDateTime timestamp = OffsetDateTime.now();
        private Map<String, Object> details;

        public ErrorDetail() {}

        public ErrorDetail(String code, String message, Integer status, String path, OffsetDateTime timestamp, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.status = status;
            this.path = path;
            this.timestamp = timestamp;
            this.details = details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }
}
