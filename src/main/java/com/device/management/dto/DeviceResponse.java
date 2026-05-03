package com.device.management.dto;

import com.device.management.common.DeviceState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String name,
        String brand,
        DeviceState state,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime createdAt,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY,
                description = "Used for optimistic locking — include in PUT/PATCH to detect concurrent modifications")
        Long version  // exposed so clients can use it for optimistic locking on PUT/PATCH
) {}