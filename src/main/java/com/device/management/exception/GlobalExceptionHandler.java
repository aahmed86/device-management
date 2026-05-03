package com.device.management.exception;

import com.device.management.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handles all typed DeviceException subclasses:
    // DeviceNotFoundException           → 404
    // DeviceInUseException              → 409
    // InvalidFilterCombinationException → 400
    @ExceptionHandler(DeviceException.class)
    public ResponseEntity<ApiError> handleDeviceException(DeviceException ex, HttpServletRequest request) {

        HttpStatus status = switch (ex.getCode()) {
            case DEVICE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DEVICE_IN_USE -> HttpStatus.CONFLICT;
            case VALIDATION_ERROR, FILTER_CONFLICT -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        log.warn("Device exception [{}] on {}: {}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(new ApiError(ex.getCode().name(), ex.getMessage(), status.value(), request.getRequestURI()));
    }

    // Bean validation failures (@Valid on request body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation error");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_ERROR", errorMessage, HttpStatus.BAD_REQUEST.value(), request.getRequestURI()));
    }

    // Invalid enum value in request body (e.g. "state": "BROKEN")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidEnum(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        "INVALID_STATE",
                        "Invalid value for device state. Allowed values: ACTIVE, INACTIVE, IN_USE",
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI()
                ));
    }

    // Two concurrent requests updated the same device — @Version mismatch
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict on {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        "CONCURRENT_MODIFICATION",
                        "The device was modified by another request. Please fetch the latest version and retry.",
                        HttpStatus.CONFLICT.value(),
                        request.getRequestURI()
                ));
    }

    // Browser auto-requests favicon.ico — not an application error, no stack trace needed
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        // Don't log — this fires for every browser favicon.ico request
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NOT_FOUND", "Resource not found: " + ex.getResourcePath(), HttpStatus.NOT_FOUND.value(), request.getRequestURI()));
    }

    // Safety net — logs full stack trace so unexpected failures are visible in logs
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: ", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI()));
    }
}
