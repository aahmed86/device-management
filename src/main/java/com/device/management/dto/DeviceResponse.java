package com.device.management.dto;

import com.device.management.common.DeviceState;
import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String name,
        String brand,
        DeviceState state,
        LocalDateTime createdAt,
        Long version
) {}