package az.fitnest.catalog.exception;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.ApiResponse;
import az.fitnest.catalog.dto.response.ApiError;
import az.fitnest.catalog.exception.BaseException;
import az.fitnest.catalog.exception.ValidationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = {BaseException.class})
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException exception, WebRequest request) {

        Map<String, Object> details = null;
        if (exception instanceof GymDependencyException dependencyException) {
            details = new HashMap<>();
            List<Map<String, String>> dependencies = dependencyException.getDependencyKeys().stream()
                    .map(key -> Map.of(
                            "code", key.replace("error.gym_dependency_", "").toUpperCase(),
                            "reason", getMessage(key)
                    ))
                    .toList();
            details.put("dependencies", dependencies);
        } else if (exception instanceof ValidationException validationException) {
            BindingResult result = validationException.getBindingResult();
            if (result != null) {
                details = new HashMap<>();
                List<Map<String, String>> fieldIssues = result.getFieldErrors().stream()
                        .map(error -> Map.of("field", error.getField(), "issue", safeMessage(error.getDefaultMessage())))
                        .toList();
                details.put("fieldIssues", fieldIssues);
            }
        }

        ApiError apiError = ApiError.builder()
                .status(exception.getHttpStatus().value())
                .code(exception.getErrorCode())
                .message(getLocalizedMessage(exception.getErrorCode(), exception.getMessage()))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();

        return ResponseEntity.status(exception.getHttpStatus()).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request) {
        BindingResult result = exception.getBindingResult();
        HashMap<String, Object> details = new HashMap<>();
        List<Map<String, String>> fieldIssues = result.getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "issue", safeMessage(error.getDefaultMessage())))
                .toList();
        details.put("fieldIssues", fieldIssues);

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("VALIDATION_ERROR")
                .message(getMessage("error.validation"))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception, WebRequest request) {
        HashMap<String, Object> details = new HashMap<>();
        List<Map<String, String>> violations = exception.getConstraintViolations().stream()
                .map(v -> Map.of("property", v.getPropertyPath().toString(), "issue", safeMessage(v.getMessage())))
                .toList();
        details.put("violations", violations);

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("CONSTRAINT_VIOLATION")
                .message(getMessage("error.constraint_violation"))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(value = {HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, WebRequest request) {
        Map<String, Object> details = new HashMap<>();
        Throwable cause = exception.getCause();

        if (cause instanceof InvalidFormatException invalidFormat) {
            String fieldPath = invalidFormat.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining("."));
            String issueMessage;
            if (invalidFormat.getTargetType().isEnum()) {
                Object[] enumConstants = invalidFormat.getTargetType().getEnumConstants();
                String accepted = java.util.Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                issueMessage = "Invalid value. Accepted values: [" + accepted + "]";
            } else {
                issueMessage = "Invalid value. Expected type: " + invalidFormat.getTargetType().getSimpleName();
            }
            details.put("fieldIssues", List.of(Map.of(
                    "field", fieldPath.isEmpty() ? "unknown" : fieldPath,
                    "rejectedValue", String.valueOf(invalidFormat.getValue()),
                    "issue", issueMessage
            )));
        } else if (cause instanceof UnrecognizedPropertyException unrecognized) {
            String fieldPath = unrecognized.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining("."));
            details.put("fieldIssues", List.of(Map.of(
                    "field", fieldPath.isEmpty() ? unrecognized.getPropertyName() : fieldPath,
                    "issue", "Unrecognized field. Known fields: " + unrecognized.getKnownPropertyIds()
            )));
        } else if (cause instanceof MismatchedInputException mismatched) {
            String fieldPath = mismatched.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining("."));
            details.put("fieldIssues", List.of(Map.of(
                    "field", fieldPath.isEmpty() ? "unknown" : fieldPath,
                    "issue", "Type mismatch. Expected type: " + (mismatched.getTargetType() != null ? mismatched.getTargetType().getSimpleName() : "unknown")
            )));
        } else if (cause instanceof JsonParseException parseException) {
            details.put("parseError", Map.of(
                    "location", "line " + parseException.getLocation().getLineNr() + ", column " + parseException.getLocation().getColumnNr(),
                    "issue", parseException.getOriginalMessage()
            ));
        } else {
            details.put("issue", exception.getMostSpecificCause().getMessage());
        }

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("BAD_REQUEST")
                .message(getMessage("error.invalid_json_format"))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(value = {AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code("ACCESS_DENIED")
                .message(getMessage("error.access_denied"))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(value = {RuntimeException.class})
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        ex.printStackTrace();

        ApiError apiError = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("RUNTIME_EXCEPTION")
                .message(ex.getMessage() != null ? safeMessage(ex.getMessage()) : getMessage("error.unexpected"))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(apiError));

    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_SERVER_ERROR")
                .message(getMessage("error.internal_server_error"))
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(apiError));
    }

    private String getLocalizedMessage(String errorCode, String defaultMessage) {
        if (errorCode == null) return safeMessage(defaultMessage);

        String key = errorCode.startsWith("error.") ? errorCode : "error." + errorCode.toLowerCase();
        try {
            return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        } catch (org.springframework.context.NoSuchMessageException e1) {
            try {
                return messageSource.getMessage(errorCode, null, LocaleContextHolder.getLocale());
            } catch (org.springframework.context.NoSuchMessageException e2) {
                return safeMessage(defaultMessage);
            }
        }
    }

    private String safeMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return getMessage("error.unexpected");
        }
        if (msg.startsWith("error.")) {
            String resolved = getMessage(msg);
            if (!resolved.equals(msg)) {
                return resolved;
            }
        }
        if (msg.equals("INVALID_CATEGORIES") || msg.equals("GYM_NOT_FOUND") || msg.equals("STORE_NOT_FOUND")) {
             String resolved = getLocalizedMessage(msg, msg);
             if (!resolved.equals(msg)) {
                 return resolved;
             }
        }
        return msg;
    }

    private String getMessage(String code) {
        return getMessage(code, null);
    }

    private String getMessage(String code, String arg) {
        try {
            return messageSource.getMessage(code, arg != null ? new Object[]{arg} : null, LocaleContextHolder.getLocale());
        } catch (org.springframework.context.NoSuchMessageException e) {
            if (code.startsWith("error.")) return code;
            try {
                return messageSource.getMessage("error.unexpected", null, LocaleContextHolder.getLocale());
            } catch (Exception ex) {
                return code;
            }
        }
    }
}
