package com.device.management.controller;

import com.device.management.dto.DevicePatchRequest;
import com.device.management.dto.DeviceRequest;
import com.device.management.dto.DeviceResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
@ActiveProfiles("test")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService service;

    @MockitoBean
    private DeviceMapper deviceMapper;

    @BeforeEach
    void setup() {
        when(deviceMapper.toEntity(any(DeviceRequest.class))).thenReturn(new Device());
        when(deviceMapper.toEntity(any(DevicePatchRequest.class))).thenReturn(new Device());

        when(deviceMapper.toResponse(any(Device.class))).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            return new DeviceResponse(
                    d.getId(),
                    d.getName(),
                    d.getBrand(),
                    d.getState(),
                    null
            );
        });
    }

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
                .andExpect(jsonPath("$.name").value("iPhone"));
    }

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

    @Test
    @DisplayName("DELETE /api/v1/device/{id} should return 204")
    void shouldDeleteDevice() throws Exception {
        mockMvc.perform(delete("/api/v1/device/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/device/{id} should return 409 when IN_USE")
    void shouldReturn409_whenDeletingInUseDevice() throws Exception {
        doThrow(new DeviceInUseException(1L))
                .when(service).deleteDevice(1L);

        mockMvc.perform(delete("/api/v1/device/1"))
                .andExpect(status().isConflict());
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
}