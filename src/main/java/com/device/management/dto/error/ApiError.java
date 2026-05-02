package com.device.management.dto.error;

import java.time.LocalDateTime;

public class ApiError {

    private final String code;
    private final String message;
    private final int status;
    private final String path;
    private final LocalDateTime timestamp;

    public ApiError(String code, String message, int status, String path) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public String getPath() {
        return path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}