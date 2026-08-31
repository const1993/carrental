package com.acme.carrental.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
            .body(new ApiError(Instant.now(), ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
            .collect(java.util.stream.Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage(),
                (a, b) -> a
            ));
        return ResponseEntity.badRequest()
            .body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR", "Request validation failed", request.getRequestURI(), fields));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expected = ex.getRequiredType() == null ? "the required type" : ex.getRequiredType().getSimpleName();
        return ResponseEntity.badRequest().body(new ApiError(
            Instant.now(), 400, "INVALID_PARAMETER",
            "Invalid value for parameter '" + ex.getName() + "'; expected " + expected,
            request.getRequestURI(), Map.of(ex.getName(), "invalid value")
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
            Instant.now(), 400, "INVALID_REQUEST_BODY", "Request body is malformed or contains an unsupported value",
            request.getRequestURI(), Map.of()
        ));
    }

    public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fields
    ) {}
}
