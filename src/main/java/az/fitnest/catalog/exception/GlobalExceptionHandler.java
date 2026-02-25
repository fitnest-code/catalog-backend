package az.fitnest.catalog.exception;

import az.fitnest.catalog.dto.ErrorResponse;
import az.fitnest.catalog.exception.BaseException;
import az.fitnest.catalog.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @ExceptionHandler(value={BaseException.class})
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException exception, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                exception.getHttpStatus().value(),
                exception.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null
        );
                
        if (exception instanceof ValidationException) {
            ValidationException validationException = (ValidationException) exception;
            BindingResult result = validationException.getBindingResult();
            if (result != null) {
                HashMap<String, Object> details = new HashMap<String, Object>();
                List<Map<String, String>> fieldIssues = result.getFieldErrors().stream().map(error -> Map.of("field", error.getField(), "issue", error.getDefaultMessage())).toList();
                details.put("fieldIssues", fieldIssues);
                errorResponse.setDetails(details);
            }
        }
        
        return ResponseEntity.status(exception.getHttpStatus().value()).body(errorResponse);
    }

    @ExceptionHandler(value={MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request) {
        BindingResult result = exception.getBindingResult();
        HashMap<String, Object> details = new HashMap<String, Object>();
        List<Map<String, String>> fieldIssues = result.getFieldErrors().stream().map(error -> Map.of("field", error.getField(), "issue", error.getDefaultMessage())).toList();
        details.put("fieldIssues", fieldIssues);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                request.getDescription(false).replace("uri=", ""),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception, WebRequest request) {
        HashMap<String, Object> details = new HashMap<String, Object>();
        List<Map<String, String>> violations = exception.getConstraintViolations().stream().map(v -> Map.of("property", v.getPropertyPath().toString(), "issue", v.getMessage())).toList();
        details.put("violations", violations);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint violation",
                request.getDescription(false).replace("uri=", ""),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request format",
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(errorResponse);
    }

    @ExceptionHandler(value={AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN.value()).body(errorResponse);
    }

    @ExceptionHandler(value={RuntimeException.class})
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponse);
    }

    @ExceptionHandler(value={Exception.class})
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(errorResponse);
    }
}

