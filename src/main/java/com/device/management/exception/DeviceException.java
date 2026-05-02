package com.device.management.exception;

import com.device.management.common.ErrorCode;

public abstract class DeviceException extends RuntimeException {
    private final ErrorCode code;

    protected DeviceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
