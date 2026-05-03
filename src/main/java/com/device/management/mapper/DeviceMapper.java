package com.device.management.mapper;

import com.device.management.dto.DevicePatchRequest;
import com.device.management.dto.DeviceRequest;
import com.device.management.dto.DeviceResponse;
import com.device.management.model.Device;
import org.springframework.stereotype.Component;

@Component
public class DeviceMapper {

    public Device toEntity(DeviceRequest request) {
        Device device = new Device();
        device.setName(request.name());
        device.setBrand(request.brand());
        device.setState(request.state());
        return device;
    }

    public Device toEntity(DevicePatchRequest request) {
        Device device = new Device();
        device.setName(request.name()); // maybe null — that's intentional
        device.setBrand(request.brand()); // maybe null — that's intentional
        device.setState(request.state()); // maybe null — that's intentional
        return device;
    }

    public DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getBrand(),
                device.getState(),
                device.getCreatedAt(),
                device.getVersion()
        );
    }
}
