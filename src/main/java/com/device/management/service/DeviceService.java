package com.device.management.service;

import com.device.management.common.DeviceState;
import com.device.management.exception.DeviceInUseException;
import com.device.management.exception.DeviceNotFoundException;
import com.device.management.model.Device;
import com.device.management.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) { this.repository = repository; }

    @Transactional
    public Device createDevice(Device device) {
        log.info("Creating device: name={}, brand={}", device.getName(), device.getBrand());
        Device saved = repository.save(device);
        log.info("Device created with id={}", saved.getId());
        return saved;
    }
    public List<Device> getAllDevices() { return repository.findAll(); }
    public List<Device> getByBrand(String brand) { return repository.findByBrand(brand); }
    public List<Device> getByState(DeviceState state) { return repository.findByState(state); }
    public Device getById(Long id) { return repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id)); }

    @Transactional
    public Device updateDevice(Long id, Device details, Long clientVersion) {
        log.info("Attempting to update details device id={}", id);
        Device existing = getById(id);

        if (clientVersion != null && !clientVersion.equals(existing.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Device.class, id);
        }

        validateUpdate(existing, details.getName(), details.getBrand());
        existing.setName(details.getName());
        existing.setBrand(details.getBrand());
        existing.setState(details.getState());
        Device updatedDevice = repository.save(existing);
        log.info("Device id={} updated successfully - updateDevice", updatedDevice.getId());
        return updatedDevice;
    }

    @Transactional
    public Device patchDevice(Long id, Device updates, Long clientVersion) {
        log.info("Attempting to partially update device details id={}", id);
        Device existing = getById(id);

        if (clientVersion != null && !clientVersion.equals(existing.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Device.class, id);
        }

        String newName = updates.getName() != null ? updates.getName() : existing.getName();
        String newBrand = updates.getBrand() != null ? updates.getBrand() : existing.getBrand();

        validateUpdate(existing, newName, newBrand);

        if (updates.getName() != null) existing.setName((updates.getName()));
        if (updates.getBrand() != null) existing.setBrand(updates.getBrand());
        if (updates.getState() != null) existing.setState(updates.getState());
        Device updatedDevice = repository.save(existing);
        log.info("Device id={} updated successfully - patchDevice", updatedDevice.getId());
        return updatedDevice;
    }

    @Transactional
    public void deleteDevice(Long id) {
        log.info("Attempting to delete device id={}", id);
        Device existing = getById(id);
        if (existing.getState() == DeviceState.IN_USE) {
            log.warn("Delete blocked: device id={} is IN_USE", id);
            throw new DeviceInUseException(id);
        }
        repository.delete(existing);
        log.info("Device id={} deleted successfully", id);
    }

    private void validateUpdate(Device existing, String newName, String newBrand) {
        if (existing.getState() == DeviceState.IN_USE) {
            boolean nameChanged = (newName != null && !newName.equals(existing.getName()));
            boolean brandChanged = (newBrand != null && !newBrand.equals(existing.getBrand()));

            if (nameChanged || brandChanged) {
                log.warn("Update blocked: device id={} is IN_USE", existing.getId());
                throw new DeviceInUseException(existing.getId());
            }
        }
    }
}