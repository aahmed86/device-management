package com.device.management.repository;

import com.device.management.model.Device;
import com.device.management.common.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByBrand(String brand);
    List<Device> findByState(DeviceState state);
}
