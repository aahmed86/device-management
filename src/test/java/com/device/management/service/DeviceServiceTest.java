package com.device.management.service;

import com.device.management.common.DeviceState;
import com.device.management.exception.DeviceInUseException;
import com.device.management.exception.DeviceNotFoundException;
import com.device.management.model.Device;
import com.device.management.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure Mockito — no Spring context, no profile needed
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository repository;

    @InjectMocks
    private DeviceService service;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createDevice — Should create device successfully")
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

    // ── READ ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById — Should return device when ID exists")
    void getById_shouldReturnDevice() {
        Device device = new Device();
        device.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(device));

        assertEquals(1L, service.getById(1L).getId());
    }

    @Test
    @DisplayName("getById — Should throw ResourceNotFoundException when device not found")
    void getById_shouldThrow_whenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class, () -> service.getById(1L));
    }

    @Test
    @DisplayName("getAllDevices — Should return all devices")
    void getAllDevices_shouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(new Device(), new Device()));
        assertEquals(2, service.getAllDevices().size());
    }

    @Test
    @DisplayName("getByBrand — Should return devices by brand")
    void getByBrand_shouldReturnFiltered() {
        Device d = new Device();
        d.setBrand("Apple");
        when(repository.findByBrand("Apple")).thenReturn(List.of(d));
        assertEquals(1, service.getByBrand("Apple").size());
    }

    @Test
    @DisplayName("getByState — Should return devices by state")
    void getByState_shouldReturnFiltered() {
        Device d = new Device();
        d.setState(DeviceState.AVAILABLE);
        when(repository.findByState(DeviceState.AVAILABLE)).thenReturn(List.of(d));
        assertEquals(1, service.getByState(DeviceState.AVAILABLE).size());
    }

    // ── FULL UPDATE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateDevice — should update all fields when device is AVAILABLE")
    void update_shouldWork_whenAvailable() {
        Device existing = new Device();
        existing.setName("Old");
        existing.setBrand("A");
        existing.setState(DeviceState.AVAILABLE);

        Device update = new Device();
        update.setName("New");
        update.setBrand("B");
        update.setState(DeviceState.INACTIVE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Device result = service.updateDevice(1L, update, null);
        assertEquals("New", result.getName());
        assertEquals("B", result.getBrand());
        assertEquals(DeviceState.INACTIVE, result.getState());
    }

    @Test
    @DisplayName("updateDevice — should throw when name changes and device is IN_USE")
    void update_shouldFail_whenNameChanged_andInUse() {
        Device existing = new Device();
        existing.setName("Old");
        existing.setBrand("A");
        existing.setState(DeviceState.IN_USE);

        Device update = new Device();
        update.setName("New");
        update.setBrand("A");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThrows(DeviceInUseException.class, () -> service.updateDevice(1L, update, null));
    }

    @Test
    @DisplayName("updateDevice — should throw when brand changes and device is IN_USE")
    void update_shouldFail_whenBrandChanged_andInUse() {
        Device existing = new Device();
        existing.setName("X");
        existing.setBrand("Old");
        existing.setState(DeviceState.IN_USE);

        Device update = new Device();
        update.setName("X");
        update.setBrand("New");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThrows(DeviceInUseException.class, () -> service.updateDevice(1L, update, null));
    }

    @Test
    @DisplayName("updateDevice — should succeed when IN_USE but name and brand unchanged")
    void update_shouldWork_whenInUse_andNothingChanged() {
        Device existing = new Device();
        existing.setName("Same");
        existing.setBrand("A");
        existing.setState(DeviceState.IN_USE);

        Device update = new Device();
        update.setName("Same");
        update.setBrand("A");
        update.setState(DeviceState.IN_USE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.updateDevice(1L, update, null));
    }

    // ── OPTIMISTIC LOCKING ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateDevice — should throw on version mismatch")
    void update_shouldFail_whenVersionMismatch() {
        Device existing = new Device();
        existing.setName("X"); existing.setBrand("A"); existing.setState(DeviceState.AVAILABLE);
        // existing.getVersion() returns null (new Device()) — but we simulate version=1 in DB
        // Use a real version by loading from repo mock
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        // existing version is null (0 effectively), client sends version=5
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> service.updateDevice(1L, new Device(), 5L));
    }

    @Test
    @DisplayName("patchDevice — should throw on version mismatch")
    void patch_shouldFail_whenVersionMismatch() {
        Device existing = new Device();
        existing.setState(DeviceState.AVAILABLE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> service.patchDevice(1L, new Device(), 5L));
    }

    // ── PARTIAL UPDATE ────────────────────────────────────────────────────────

    @Test
    @DisplayName("patchDevice — should update only state when device is IN_USE")
    void patch_shouldUpdateState_whenInUse() {
        Device existing = new Device();
        existing.setName("X");
        existing.setBrand("Y");
        existing.setState(DeviceState.IN_USE);

        Device updates = new Device();
        updates.setState(DeviceState.INACTIVE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Device result = service.patchDevice(1L, updates, null);
        assertEquals(DeviceState.INACTIVE, result.getState());
        // name and brand untouched
        assertEquals("X", result.getName());
        assertEquals("Y", result.getBrand());
    }

    @Test
    @DisplayName("patchDevice — should update only name when device is AVAILABLE")
    void patch_shouldUpdateName_whenAvailable() {
        Device existing = new Device();
        existing.setName("Old");
        existing.setBrand("B");
        existing.setState(DeviceState.AVAILABLE);

        Device updates = new Device();
        updates.setName("New");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Device result = service.patchDevice(1L, updates, null);
        assertEquals("New", result.getName());
        assertEquals("B", result.getBrand()); // unchanged
    }

    @Test
    @DisplayName("patchDevice — should throw when name changes and device is IN_USE")
    void patch_shouldFail_whenNameChanged_andInUse() {
        Device existing = new Device();
        existing.setName("Old");
        existing.setBrand("A");
        existing.setState(DeviceState.IN_USE);

        Device updates = new Device();
        updates.setName("New");

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        assertThrows(DeviceInUseException.class, () -> service.patchDevice(1L, updates, null));
    }

    @Test
    @DisplayName("patchDevice - Should allow state change via patch even when IN_USE")
    void patch_shouldUpdateState() {
        Device existing = new Device();
        existing.setState(DeviceState.IN_USE);

        Device updates = new Device();
        updates.setState(DeviceState.INACTIVE);

        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Device result = service.patchDevice(1L, updates, null);

        assertEquals(DeviceState.INACTIVE, result.getState());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteDevice — should delete when device is AVAILABLE")
    void delete_shouldWork_whenAvailable() {
        Device device = new Device();
        device.setState(DeviceState.AVAILABLE);
        when(repository.findById(1L)).thenReturn(Optional.of(device));

        service.deleteDevice(1L);
        verify(repository).delete(device);
    }

    @Test
    @DisplayName("deleteDevice — should delete when device is INACTIVE")
    void delete_shouldWork_whenInactive() {
        Device device = new Device();
        device.setState(DeviceState.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(device));

        service.deleteDevice(1L);
        verify(repository).delete(device);
    }

    @Test
    @DisplayName("deleteDevice — should throw DeviceInUseException when device is IN_USE")
    void delete_shouldThrow_whenInUse() {
        Device device = new Device();
        device.setState(DeviceState.IN_USE);
        when(repository.findById(1L)).thenReturn(Optional.of(device));

        assertThrows(DeviceInUseException.class, () -> service.deleteDevice(1L));
        verify(repository, never()).delete(any());
    }
}