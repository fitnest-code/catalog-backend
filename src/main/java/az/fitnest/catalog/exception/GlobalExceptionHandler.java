package az.fitnest.catalog.exception;

import az.fitnest.catalog.dto.ErrorResponse;
import az.fitnest.catalog.exception.BaseException;
import az.fitnest.catalog.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value={BaseException.class})
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException exception, WebRequest request) {
        log.warn("BaseException occurred: code={}, message={}, path={}", exception.getErrorCode(), exception.getMessage(), request.getDescription(false));
        
        ErrorResponse.ErrorDetail.ErrorDetailBuilder builder = ErrorResponse.ErrorDetail.builder()
                .message(exception.getMessage())
                .code(exception.getErrorCode())
                .status(exception.getHttpStatus().value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(null);
                
        if (exception instanceof ValidationException) {
            ValidationException validationException = (ValidationException) exception;
            BindingResult result = validationException.getBindingResult();
            if (result != null) {
                HashMap<String, Object> details = new HashMap<String, Object>();
                List<Map<String, String>> fieldIssues = result.getFieldErrors().stream().map(error -> Map.of("field", error.getField(), "issue", error.getDefaultMessage())).toList();
                details.put("fieldIssues", fieldIssues);
                builder.details(details);
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder().error(builder.build()).build();
        return ResponseEntity.status(exception.getHttpStatus().value()).body(errorResponse);
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request) {
        log.warn("MethodArgumentNotValidException: path={}", request.getDescription(false));
        BindingResult result = exception.getBindingResult();
        HashMap<String, Object> details = new HashMap<String, Object>();
        List<Map<String, String>> fieldIssues = result.getFieldErrors().stream().map(error -> Map.of("field", error.getField(), "issue", error.getDefaultMessage())).toList();
        details.put("fieldIssues", fieldIssues);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .message("Validation failed")
                        .code("VALIDATION_ERROR")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(OffsetDateTime.now())
                        .details(details)
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception, WebRequest request) {
        log.warn("ConstraintViolationException: path={}", request.getDescription(false));
        HashMap<String, Object> details = new HashMap<String, Object>();
        List<Map<String, String>> violations = exception.getConstraintViolations().stream().map(v -> Map.of("property", v.getPropertyPath().toString(), "issue", v.getMessage())).toList();
        details.put("violations", violations);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .message("Constraint violation")
                        .code("CONSTRAINT_VIOLATION")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(OffsetDateTime.now())
                        .details(details)
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, WebRequest request) {
        log.warn("HttpMessageNotReadableException: message={}, path={}", exception.getMessage(), request.getDescription(false));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .message("Invalid request format")
                        .code("HTTP_MESSAGE_NOT_READABLE")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(OffsetDateTime.now())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("AccessDeniedException: path={}", request.getDescription(false));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .message("Access denied")
                        .code("ACCESS_DENIED")
                        .status(HttpStatus.FORBIDDEN.value())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(OffsetDateTime.now())
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN.value()).body(errorResponse);
    }

    @ExceptionHandler(value={RuntimeException.class})
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("RuntimeException occurred: path={}", request.getDescription(false), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .message("Internal server error: " + ex.getMessage())
                        .code("RUNTIME_EXCEPTION")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(OffsetDateTime.now())
                        .details(Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null"))
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponse);
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled Exception occurred: path={}", request.getDescription(false), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(ErrorResponse.ErrorDetail.builder()
                        .message("Internal server error: " + ex.getMessage())
                        .code("INTERNAL_SERVER_ERROR")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(OffsetDateTime.now())
                        .details(Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null"))
                        .build())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponse);
    }
}

