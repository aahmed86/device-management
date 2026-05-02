package com.device.management.service;

import com.device.management.exception.DeviceInUseException;
import com.device.management.exception.DeviceNotFoundException;
import com.device.management.model.Device;
import com.device.management.common.DeviceState;
import com.device.management.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DeviceServiceTest {

    @Mock
    private DeviceRepository repository;

    @InjectMocks
    private DeviceService service;

    @Test
    @DisplayName("Should create device successfully")
    void createDevice_shouldSave() {
        Device device = new Device();
        device.setName("iPhone");
        device.setBrand("Apple");
        device.setState(DeviceState.AVAILABLE);

        when(repository.save(device)).thenReturn(device);

        Device result = service.createDevice(device);

        assertEquals("iPhone", result.getName());
        verify(repository).save(device);
    }

    @Test
    @DisplayName("Should return device when ID exists")
    void getById_shouldReturnDevice() {
        Device device = new Device();
        device.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(device));

        Device result = service.getById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when device not found")
    void getById_shouldThrow_whenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    @DisplayName("Should prevent deletion when device is IN_USE")
    void delete_shouldThrow_whenInUse() {
        Device device = new Device();
        device.setState(DeviceState.IN_USE);

        when(repository.findById(1L)).thenReturn(Optional.of(device));

        assertThrows(DeviceInUseException.class, () -> service.deleteDevice(1L));
    }

    @Test
    @DisplayName("Should allow deletion when device is AVAILABLE")
    void delete_shouldWork_whenAvailable() {
        Device device = new Device();
        device.setState(DeviceState.AVAILABLE);

        when(repository.findById(1L)).thenReturn(Optional.of(device));

        service.deleteDevice(1L);

        verify(repository).delete(device);
    }

    @Test
    @DisplayName("Should block name change when device is IN_USE")
    void update_shouldFail_whenNameChanged_andInUse() {
        Device existing = new Device();
        existing.setName("Old");
        existing.setBrand("A");
        existing.setState(DeviceState.IN_USE);

        Device update = new Device();
        update.setName("New");
        update.setBrand("A");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(DeviceInUseException.class, () -> service.updateDevice(1L, update));
    }

    @Test
    @DisplayName("Should allow state change via patch even when IN_USE")
    void patch_shouldUpdateState() {
        Device existing = new Device();
        existing.setState(DeviceState.IN_USE);

        Device updates = new Device();
        updates.setState(DeviceState.INACTIVE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Device result = service.patchDevice(1L, updates);

        assertEquals(DeviceState.INACTIVE, result.getState());
    }

    @Test
    @DisplayName("Should allow full update when device is AVAILABLE")
    void update_shouldWork_whenAvailable() {
        Device existing = new Device();
        existing.setName("Old"); existing.setBrand("A"); existing.setState(DeviceState.AVAILABLE);
        Device update = new Device();
        update.setName("New"); update.setBrand("B"); update.setState(DeviceState.INACTIVE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Device result = service.updateDevice(1L, update);
        assertEquals("New", result.getName());
    }

    @Test
    @DisplayName("Should allow update when IN_USE but name/brand unchanged")
    void update_shouldWork_whenInUse_andNothingChanged() {
        Device existing = new Device();
        existing.setName("Same"); existing.setBrand("A"); existing.setState(DeviceState.IN_USE);
        Device update = new Device();
        update.setName("Same"); update.setBrand("A"); update.setState(DeviceState.IN_USE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.updateDevice(1L, update));
    }

    @Test
    @DisplayName("Should return all devices")
    void getAllDevices_shouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(new Device(), new Device()));
        assertEquals(2, service.getAllDevices().size());
    }

    @Test
    @DisplayName("Should return devices by brand")
    void getByBrand_shouldReturnFiltered() {
        Device d = new Device(); d.setBrand("Apple");
        when(repository.findByBrand("Apple")).thenReturn(List.of(d));
        assertEquals(1, service.getByBrand("Apple").size());
    }

    @Test
    @DisplayName("Should return devices by state")
    void getByState_shouldReturnFiltered() {
        Device d = new Device(); d.setState(DeviceState.AVAILABLE);
        when(repository.findByState(DeviceState.AVAILABLE)).thenReturn(List.of(d));
        assertEquals(1, service.getByState(DeviceState.AVAILABLE).size());
    }

    @Test
    @DisplayName("Should block brand change when device is IN_USE")
    void update_shouldFail_whenBrandChanged_andInUse() {
        Device existing = new Device();
        existing.setName("X"); existing.setBrand("Old"); existing.setState(DeviceState.IN_USE);
        Device update = new Device();
        update.setName("X"); update.setBrand("New");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThrows(DeviceInUseException.class, () -> service.updateDevice(1L, update));
    }

    @Test
    @DisplayName("Should delete device when INACTIVE")
    void delete_shouldWork_whenInactive() {
        Device device = new Device(); device.setState(DeviceState.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(device));
        service.deleteDevice(1L);
        verify(repository).delete(device);
    }
}