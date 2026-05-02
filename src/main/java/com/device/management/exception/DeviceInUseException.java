package com.device.management.exception;

import com.device.management.common.ErrorCode;

public class DeviceInUseException extends DeviceException {
    public DeviceInUseException(Long id) {
        super(ErrorCode.DEVICE_IN_USE, "Device " + id + " cannot be modified while IN_USE");
    }
}
