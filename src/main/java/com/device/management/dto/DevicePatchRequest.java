package com.device.management.dto;

import com.device.management.common.DeviceState;

// all fields optional
public record DevicePatchRequest(
        String name,
        String brand,
        DeviceState state
) {}
