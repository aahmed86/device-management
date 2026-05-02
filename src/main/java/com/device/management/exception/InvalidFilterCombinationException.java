package com.device.management.exception;

import com.device.management.common.ErrorCode;

public class InvalidFilterCombinationException extends DeviceException {

    public InvalidFilterCombinationException() {
        super(ErrorCode.FILTER_CONFLICT, "Only one filter parameter (brand or state) is allowed at a time");
    }
}