/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.validation.ConstraintViolationException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.converter.HttpMessageNotReadableException
 *  org.springframework.security.access.AccessDeniedException
 *  org.springframework.validation.BindingResult
 *  org.springframework.web.bind.MethodArgumentNotValidException
 *  org.springframework.web.bind.annotation.ExceptionHandler
 *  org.springframework.web.bind.annotation.RestControllerAdvice
 *  org.springframework.web.context.request.WebRequest
 */
package az.fitnest.catalog.exception;

import az.fitnest.catalog.dto.ErrorResponse;
import az.fitnest.catalog.exception.BaseException;
import az.fitnest.catalog.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
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
        ValidationException validationException;
        BindingResult result;
        log.warn("BaseException occurred: code={}, message={}, path={}", new Object[]{exception.getErrorCode(), exception.getMessage(), request.getDescription(false)});
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder().message(exception.getMessage()).code(exception.getErrorCode()).path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).details(null);
        if (exception instanceof ValidationException && (result = (validationException = (ValidationException)exception).getBindingResult()) != null) {
            HashMap<String, Object> details = new HashMap<String, Object>();
            List<Map<String, String>> fieldIssues = result.getFieldErrors().stream().map(error -> Map.of("field", error.getField(), "issue", error.getDefaultMessage())).toList();
            details.put("fieldIssues", fieldIssues);
            builder.details(details);
        }
        ErrorResponse errorResponse = builder.build();
        return ResponseEntity.status(exception.getHttpStatus().value()).body(errorResponse);
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request) {
        log.warn("MethodArgumentNotValidException: path={}", request.getDescription(false));
        BindingResult result = exception.getBindingResult();
        HashMap<String, Object> details = new HashMap<String, Object>();
        List<Map<String, String>> fieldIssues = result.getFieldErrors().stream().map(error -> Map.of("field", error.getField(), "issue", error.getDefaultMessage())).toList();
        details.put("fieldIssues", fieldIssues);
        ErrorResponse errorResponse = ErrorResponse.builder().message("Validation failed").code("VALIDATION_ERROR").path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).details(details).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception, WebRequest request) {
        log.warn("ConstraintViolationException: path={}", request.getDescription(false));
        HashMap<String, Object> details = new HashMap<String, Object>();
        List<Map<String, String>> violations = exception.getConstraintViolations().stream().map(v -> Map.of("property", v.getPropertyPath().toString(), "issue", v.getMessage())).toList();
        details.put("violations", violations);
        ErrorResponse errorResponse = ErrorResponse.builder().message("Constraint violation").code("CONSTRAINT_VIOLATION").path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).details(details).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, WebRequest request) {
        log.warn("HttpMessageNotReadableException: message={}, path={}", exception.getMessage(), request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder().message("Invalid request format").code("HTTP_MESSAGE_NOT_READABLE").path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("AccessDeniedException: path={}", request.getDescription(false));
        ErrorResponse errorResponse = ErrorResponse.builder().message("Access denied").code("ACCESS_DENIED").path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN.value()).body(errorResponse);
    }

    @ExceptionHandler(value={RuntimeException.class})
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("RuntimeException occurred: path={}", request.getDescription(false), ex);
        ErrorResponse errorResponse = ErrorResponse.builder().message("Internal server error: " + ex.getMessage()).code("RUNTIME_EXCEPTION").path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).details(Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null")).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponse);
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled Exception occurred: path={}", request.getDescription(false), ex);
        ErrorResponse errorResponse = ErrorResponse.builder().message("Internal server error: " + ex.getMessage()).code("INTERNAL_SERVER_ERROR").path(request.getDescription(false).replace("uri=", "")).timestamp(LocalDateTime.now()).details(Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null")).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponse);
    }
}

