package com.device.management.dto;


import com.device.management.common.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Brand is required")
        String brand,
        @NotNull(message = "State is required")
        DeviceState state
) {}