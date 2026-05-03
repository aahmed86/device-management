package com.device.management.dto;

import com.device.management.common.DeviceState;

// all fields are intentionally optional
public record DevicePatchRequest(
        String name,
        String brand,
        DeviceState state,
        Long version    // nullable — include to opt into optimistic lock check
) {}
