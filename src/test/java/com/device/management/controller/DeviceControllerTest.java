package com.device.management.controller;

import com.device.management.common.DeviceState;
import com.device.management.dto.DevicePatchRequest;
import com.device.management.dto.DeviceRequest;
import com.device.management.dto.DeviceResponse;
import com.device.management.dto.DeviceUpdateRequest;
import com.device.management.exception.DeviceInUseException;
import com.device.management.exception.DeviceNotFoundException;
import com.device.management.mapper.DeviceMapper;
import com.device.management.model.Device;
import com.device.management.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest loads only the web layer — no datasource, no profile needed
@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService service;

    @MockitoBean
    private DeviceMapper deviceMapper;

    @BeforeEach
    void setup() {
        // Use typed matchers to avoid ambiguous overload resolution between
        // toEntity(DeviceRequest) and toEntity(DevicePatchRequest)
        when(deviceMapper.toEntity(any(DeviceRequest.class))).thenReturn(new Device());
        when(deviceMapper.toEntity(any(DeviceUpdateRequest.class))).thenReturn(new Device());
        when(deviceMapper.toEntity(any(DevicePatchRequest.class))).thenReturn(new Device());

        when(deviceMapper.toResponse(any(Device.class))).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            return new DeviceResponse(d.getId(), d.getName(), d.getBrand(), d.getState(), null, null);
        });
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/device should create a device")
    void shouldCreateDevice() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setName("iPhone");
        device.setBrand("Apple");

        when(service.createDevice(any())).thenReturn(device);

        mockMvc.perform(post("/api/v1/device")
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "name": "iPhone",
                          "brand": "Apple",
                          "state": "AVAILABLE"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("iPhone"))
                .andExpect(jsonPath("$.brand").value("Apple"));
    }

    @Test
    @DisplayName("POST /api/v1/device should return 400 on invalid input")
    void shouldReturn400_onInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType(APPLICATION_JSON)
                        .content("""
                    { "name": "", "brand": "", "state": null }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/device — should return 400 on invalid state value")
    void shouldReturn400_onInvalidState() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "name": "X", "brand": "Y", "state": "BROKEN" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/device should return all devices")
    void shouldReturnAllDevices() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setName("Pixel");

        when(service.getAllDevices()).thenReturn(List.of(device));

        mockMvc.perform(get("/api/v1/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pixel"));
    }

    @Test
    @DisplayName("GET /api/v1/device?brand=Apple should filter by brand")
    void shouldFilterByBrand() throws Exception {
        Device d = new Device();
        d.setId(1L);
        d.setName("iPhone");
        d.setBrand("Apple");
        when(service.getByBrand("Apple")).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/device?brand=Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Apple"));
    }

    @Test
    @DisplayName("GET /api/v1/device?state=AVAILABLE — should filter by state")
    void shouldFilterByState() throws Exception {
        Device d = new Device();
        d.setId(1L);
        d.setState(DeviceState.AVAILABLE);
        when(service.getByState(DeviceState.AVAILABLE)).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/device?state=AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/device — should return 400 when both brand and state are provided")
    void shouldReturn400_whenBothFiltersUsed() throws Exception {
        mockMvc.perform(get("/api/v1/device?brand=Apple&state=AVAILABLE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/device/{id} should return device")
    void shouldReturnDeviceById() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setName("iPhone");
        when(service.getById(1L)).thenReturn(device);

        mockMvc.perform(get("/api/v1/device/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"));
    }

    @Test
    @DisplayName("GET /api/v1/device/{id} should return 404 when not found")
    void shouldReturn404_whenDeviceNotFound() throws Exception {
        when(service.getById(99L)).thenThrow(new DeviceNotFoundException(99L));

        mockMvc.perform(get("/api/v1/device/99"))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/v1/device/{id} — should return updated device")
    void shouldFullyUpdateDevice() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setName("Updated");
        device.setBrand("Samsung");
        device.setState(DeviceState.INACTIVE);
        when(service.updateDevice(any(Long.class), any(Device.class), any())).thenReturn(device);

        mockMvc.perform(put("/api/v1/device/1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "name": "Updated", "brand": "Samsung", "state": "INACTIVE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @DisplayName("PUT /api/v1/device/{id} — should return 409 when device is IN_USE and name changes")
    void shouldReturn409_onFullUpdate_whenInUse() throws Exception {
        when(service.updateDevice(any(Long.class), any(Device.class), any()))
                .thenThrow(new DeviceInUseException(1L));

        mockMvc.perform(put("/api/v1/device/1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "name": "New Name", "brand": "Apple", "state": "IN_USE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_IN_USE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/device/{id} — should return patched device")
    void shouldPartiallyUpdateDevice() throws Exception {
        Device device = new Device();
        device.setId(1L);
        device.setState(DeviceState.INACTIVE);
        when(service.patchDevice(any(Long.class), any(Device.class), any())).thenReturn(device);

        mockMvc.perform(patch("/api/v1/device/1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "state": "INACTIVE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("INACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/device/{id} — should return 409 when device is IN_USE and name changes")
    void shouldReturn409_onPatch_whenInUse() throws Exception {
        when(service.patchDevice(any(Long.class), any(Device.class), any()))
                .thenThrow(new DeviceInUseException(1L));

        mockMvc.perform(patch("/api/v1/device/1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "name": "New Name" }
                                """))
                .andExpect(status().isConflict());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/device/{id} should return 204")
    void shouldDeleteDevice() throws Exception {
        mockMvc.perform(delete("/api/v1/device/1")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/device/{id} should return 409 when device is IN_USE")
    void shouldReturn409_whenDeletingInUseDevice() throws Exception {
        doThrow(new DeviceInUseException(1L)).when(service).deleteDevice(1L);

        mockMvc.perform(delete("/api/v1/device/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_IN_USE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/device/{id} — should return 404 when device not found")
    void shouldReturn404_whenDeletingNonExistentDevice() throws Exception {
        doThrow(new DeviceNotFoundException(99L)).when(service).deleteDevice(99L);

        mockMvc.perform(delete("/api/v1/device/99"))
                .andExpect(status().isNotFound());
    }
}