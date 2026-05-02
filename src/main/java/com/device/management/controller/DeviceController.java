package com.device.management.controller;

import com.device.management.dto.DevicePatchRequest;
import com.device.management.dto.DeviceResponse;
import com.device.management.dto.error.ApiError;
import com.device.management.exception.InvalidFilterCombinationException;
import com.device.management.mapper.DeviceMapper;
import com.device.management.model.Device;
import com.device.management.dto.DeviceRequest;
import com.device.management.common.DeviceState;
import com.device.management.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Devices", description = "Device management operations")
@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {
    private final DeviceService service;
    private final DeviceMapper deviceMapper;

    public DeviceController(DeviceService service, DeviceMapper deviceMapper) {
        this.service = service;
        this.deviceMapper = deviceMapper;
    }

    @Operation(
            summary = "Create a new device",
            description = "Creates a new device in the system using the provided details. " +
                    "The device will be stored with its initial state and automatically assigned an ID and creation timestamp.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Device created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DeviceResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data (validation failure)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    )
            })
    @PostMapping
    public ResponseEntity<DeviceResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Device data to create",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DeviceRequest.class))
            )
            @Valid @RequestBody DeviceRequest device) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceMapper.toResponse(service.createDevice(deviceMapper.toEntity(device))));
    }

    @Operation(
            summary = "Get all devices (with optional filters)",
            description = "Retrieve all devices. You can optionally filter by brand OR state (mutually exclusive / not both).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Devices retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            minItems = 0,
                                            uniqueItems = false,
                                            schema = @Schema(implementation = DeviceResponse.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid filter combination (brand and state cannot be used together)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    )
            }
    )
    @GetMapping
    public List<DeviceResponse> fetchAll(@RequestParam(required = false) String brand,
                                         @RequestParam(required = false) DeviceState state) {
        if (brand != null && state != null) {
            throw new InvalidFilterCombinationException();
        }

        List<Device> devices;

        if (brand != null) devices = service.getByBrand(brand);
        else if (state != null) devices = service.getByState(state);
        else devices = service.getAllDevices();

        return devices.stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Get device by ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Device found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DeviceResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Device not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    )
            }
    )
    @GetMapping("/{id}")
    public DeviceResponse getById(@PathVariable Long id) { return deviceMapper.toResponse(service.getById(id)); }

    @Operation(
            summary = "Fully update a device",
            description = "Replaces all fields of an existing device, replaces device name, brand, and state. " +
                    "Business rules: name and brand cannot be modified if the device is IN_USE. " +
                    "The creation timestamp is system-managed and cannot be changed.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Device updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DeviceResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or validation error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Device not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Device is currently in use and cannot be modified",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    )
            }
    )
    @PutMapping("/{id}")
    public DeviceResponse fullUpdate(@PathVariable Long id,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                             description = "Full device data for update",
                                             required = true,
                                             content = @Content(schema = @Schema(implementation = DeviceRequest.class))
                                     )
                                     @Valid @RequestBody DeviceRequest device) {
        return deviceMapper.toResponse(service.updateDevice(id, deviceMapper.toEntity(device)));
    }

    @Operation(
            summary = "Partially update a device",
            description = "Updates only provided fields of a device, updates selected device fields (name, brand, state). " +
                    "Business rules: name and brand cannot be updated when the device is IN_USE. " +
                    "Invalid state values are rejected.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Device partially updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DeviceResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or invalid state value",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Device not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Device is currently in use and restricted fields cannot be updated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    )
            }
    )
    @PatchMapping("/{id}")
    public DeviceResponse partialUpdate(@PathVariable Long id,
                                        @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                description = "Fields to update (partial)",
                                                required = true,
                                                content = @Content(schema = @Schema(implementation = DevicePatchRequest.class))
                                        )
                                        @RequestBody DevicePatchRequest updates) {
        return deviceMapper.toResponse(service.patchDevice(id, deviceMapper.toEntity(updates)));
    }

    @Operation(
            summary = "Delete a device",
            description = "Deletes an existing device from the system by its unique ID. Once deleted, the device cannot be recovered. " +
                    "Business rule: devices in IN_USE state cannot be deleted.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Device successfully deleted"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Device not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Device is currently in use and cannot be deleted",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiError.class)
                            )
                    )
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}