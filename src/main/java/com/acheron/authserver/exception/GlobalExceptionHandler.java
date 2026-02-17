package com.acheron.authserver.exception;

import com.acheron.authserver.dto.util.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex, HttpServletRequest request) {
        // Create the ApiError object
        ApiError apiError = new ApiError(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(apiError, ex.getStatus());
    }

    /**
     * Handles validation errors (@Valid, @NotNull, etc.).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        String path = request.getRequestURI();
        log.warn("Validation failed at {}: {}", path, errors);

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                errors,
                path
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handles database duplicates (e.g., registration with an existing email).
     * Returns 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        String message = "Data integrity violation";

        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
            String rootMsg = ex.getRootCause().getMessage().toLowerCase();
            if (rootMsg.contains("duplicate") || rootMsg.contains("unique")) {
                message = "Value already exists (duplicate key violation)";
            }
        }

        log.error("Database conflict at {}: {}", path, message);

        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    /**
     * Handles JSON parsing errors (e.g., malformed JSON request).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonErrors(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        String message = "Malformed JSON request or invalid data format";
        log.warn("JSON parse error at {}: {}", path, ex.getMessage());

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.badRequest().body(apiError);
    }

    /**
     * Handles IllegalArgumentException (logical client errors).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.warn("Illegal argument at {}: {}", path, ex.getMessage());

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                path
        );

        return ResponseEntity.badRequest().body(apiError);
    }
}