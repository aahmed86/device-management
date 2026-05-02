package com.device.management.exception;

import com.device.management.common.ErrorCode;

public class DeviceNotFoundException extends DeviceException {
    public DeviceNotFoundException(Long id) {
        super(ErrorCode.DEVICE_NOT_FOUND, "Device not found with id " + id);
    }
}
