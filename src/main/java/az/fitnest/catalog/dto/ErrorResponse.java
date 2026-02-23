/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {
    private String message;
    private String code;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, Object> details;

    public static ErrorResponse of(String message, String code) {
        return ErrorResponse.builder().message(message).code(code).timestamp(LocalDateTime.now()).build();
    }

    public static ErrorResponse of(String message, String code, String path) {
        return ErrorResponse.builder().message(message).code(code).path(path).timestamp(LocalDateTime.now()).build();
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public String getMessage() {
        return this.message;
    }

    public String getCode() {
        return this.code;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public String getPath() {
        return this.path;
    }

    public Map<String, Object> getDetails() {
        return this.details;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ErrorResponse)) {
            return false;
        }
        ErrorResponse other = (ErrorResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$message = this.getMessage();
        String other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) {
            return false;
        }
        String this$code = this.getCode();
        String other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
            return false;
        }
        LocalDateTime this$timestamp = this.getTimestamp();
        LocalDateTime other$timestamp = other.getTimestamp();
        if (this$timestamp == null ? other$timestamp != null : !((Object)this$timestamp).equals(other$timestamp)) {
            return false;
        }
        String this$path = this.getPath();
        String other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) {
            return false;
        }
        Map<String, Object> this$details = this.getDetails();
        Map<String, Object> other$details = other.getDetails();
        return !(this$details == null ? other$details != null : !((Object)this$details).equals(other$details));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ErrorResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        String $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        LocalDateTime $timestamp = this.getTimestamp();
        result = result * 59 + ($timestamp == null ? 43 : ((Object)$timestamp).hashCode());
        String $path = this.getPath();
        result = result * 59 + ($path == null ? 43 : $path.hashCode());
        Map<String, Object> $details = this.getDetails();
        result = result * 59 + ($details == null ? 43 : ((Object)$details).hashCode());
        return result;
    }

    public String toString() {
        return "ErrorResponse(message=" + this.getMessage() + ", code=" + this.getCode() + ", timestamp=" + this.getTimestamp() + ", path=" + this.getPath() + ", details=" + this.getDetails() + ")";
    }

    public ErrorResponse() {
    }

    public ErrorResponse(String message, String code, LocalDateTime timestamp, String path, Map<String, Object> details) {
        this.message = message;
        this.code = code;
        this.timestamp = timestamp;
        this.path = path;
        this.details = details;
    }

    public static class ErrorResponseBuilder {
        private String message;
        private String code;
        private LocalDateTime timestamp;
        private String path;
        private Map<String, Object> details;

        ErrorResponseBuilder() {
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this.message, this.code, this.timestamp, this.path, this.details);
        }

        public String toString() {
            return "ErrorResponse.ErrorResponseBuilder(message=" + this.message + ", code=" + this.code + ", timestamp=" + this.timestamp + ", path=" + this.path + ", details=" + this.details + ")";
        }
    }
}

