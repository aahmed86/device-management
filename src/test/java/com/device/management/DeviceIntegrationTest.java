package com.device.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Resets the Spring context (and in-memory DB) before each test to prevent state leaking between tests
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DeviceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── helpers ───────────────────────────────────────────────────────────────

    private JsonNode createDevice(String name, String brand, String state) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                                { "name": "%s", "brand": "%s", "state": "%s" }
                                """.formatted(name, brand, state)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // ── VALIDATION ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST — should return 400 when required fields are blank")
    void shouldReturn400_whenInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                                { "name": "", "brand": "", "state": null }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST — should return 400 when state value is invalid")
    void shouldReturn400_whenInvalidState() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                                { "name": "Test", "brand": "Brand", "state": "BROKEN" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── CREATE + READ ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST then GET — should create and retrieve device end-to-end")
    void shouldCreateAndRetrieveDevice_endToEnd() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                                { "name": "Pixel", "brand": "Google", "state": "AVAILABLE" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Pixel"))
                .andExpect(jsonPath("$.brand").value("Google"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(get("/api/v1/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Pixel"));
    }

    @Test
    @DisplayName("GET /{id} — should return 404 when device does not exist")
    void shouldReturn404_whenDeviceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/device/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEVICE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET — should filter devices by brand")
    void shouldFilterByBrand() throws Exception {
        createDevice("iPhone", "Apple", "AVAILABLE");

        mockMvc.perform(get("/api/v1/device?brand=Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Apple"));
    }

    @Test
    @DisplayName("GET — should filter devices by state")
    void shouldFilterByState() throws Exception {
        createDevice("Watch", "Apple", "IN_USE");

        mockMvc.perform(get("/api/v1/device?state=IN_USE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("IN_USE"));
    }

    @Test
    @DisplayName("GET — should return 400 when both brand and state filters are used")
    void shouldReturn400_whenBothFiltersUsed() throws Exception {
        mockMvc.perform(get("/api/v1/device?brand=Apple&state=AVAILABLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILTER_CONFLICT"));
    }

    // ── FULL UPDATE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT — should fully update an AVAILABLE device")
    void shouldFullyUpdateDevice() throws Exception {
        JsonNode created = createDevice("Old", "OldBrand", "AVAILABLE");
        long id = created.get("id").asLong();

        mockMvc.perform(put("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "name": "New", "brand": "NewBrand", "state": "INACTIVE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.brand").value("NewBrand"))
                .andExpect(jsonPath("$.state").value("INACTIVE"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("PUT — should return 409 when trying to change name of IN_USE device")
    void shouldReturn409_onFullUpdate_whenInUse() throws Exception {
        JsonNode created = createDevice("Locked", "Brand", "IN_USE");
        long id = created.get("id").asLong();

        mockMvc.perform(put("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "name": "Changed", "brand": "Brand", "state": "IN_USE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_IN_USE"));
    }

    // ── OPTIMISTIC LOCKING ────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT — should succeed when correct version is provided")
    void shouldUpdateDevice_whenCorrectVersionProvided() throws Exception {
        JsonNode created = createDevice("Device", "Brand", "AVAILABLE");
        long id = created.get("id").asLong();
        long version = created.get("version").asLong(); // version = 0

        mockMvc.perform(put("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "name": "Updated", "brand": "Brand", "state": "AVAILABLE", "version": %d }
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.version").value(version + 1)); // 0 → 1
    }

    @Test
    @DisplayName("PUT — should return 409 when stale version is provided (concurrent modification)")
    void shouldReturn409_whenStaleVersionOnPut() throws Exception {
        JsonNode created = createDevice("Device", "Brand", "AVAILABLE");
        long id = created.get("id").asLong();

        // First update — moves version from 0 → 1
        mockMvc.perform(put("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "name": "First Update", "brand": "Brand", "state": "AVAILABLE", "version": 0 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // Second update with stale version 0 — should be rejected
        mockMvc.perform(put("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "name": "Stale Update", "brand": "Brand", "state": "AVAILABLE", "version": 0 }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("PATCH — should return 409 when stale version is provided")
    void shouldReturn409_whenStaleVersionOnPatch() throws Exception {
        JsonNode created = createDevice("Device", "Brand", "AVAILABLE");
        long id = created.get("id").asLong();

        // First patch — moves version 0 → 1
        mockMvc.perform(patch("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "state": "IN_USE", "version": 0 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // Second patch with stale version 0 — should be rejected
        mockMvc.perform(patch("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "state": "INACTIVE", "version": 0 }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("PUT — should succeed without version (optimistic lock check skipped)")
    void shouldUpdateDevice_whenNoVersionProvided() throws Exception {
        JsonNode created = createDevice("Device", "Brand", "AVAILABLE");
        long id = created.get("id").asLong();

        // No version field — update is applied unconditionally
        mockMvc.perform(put("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "name": "NoVersion", "brand": "Brand", "state": "AVAILABLE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NoVersion"));
    }

    // ── PARTIAL UPDATE ────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH — should update only state of an IN_USE device")
    void shouldPatchStateOnly_whenInUse() throws Exception {
        JsonNode created = createDevice("Device", "Brand", "IN_USE");
        long id = created.get("id").asLong();

        mockMvc.perform(patch("/api/v1/device/" + id)
                        .contentType("application/json")
                        .content("""
                                { "state": "INACTIVE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("INACTIVE"))
                .andExpect(jsonPath("$.name").value("Device"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE — should return 204 and device should no longer be retrievable")
    void shouldDeleteAvailableDevice() throws Exception {
        JsonNode created = createDevice("ToDelete", "Brand", "AVAILABLE");
        long id = created.get("id").asLong();

        mockMvc.perform(delete("/api/v1/device/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/device/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE — should return 409 when device is IN_USE")
    void shouldReturn409_whenDeletingInUseDevice() throws Exception {
        JsonNode created = createDevice("InUse", "Brand", "IN_USE");
        long id = created.get("id").asLong();

        mockMvc.perform(delete("/api/v1/device/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_IN_USE"));
    }

    @Test
    @DisplayName("DELETE — should return 404 when device does not exist")
    void shouldReturn404_whenDeletingNonExistentDevice() throws Exception {
        mockMvc.perform(delete("/api/v1/device/999"))
                .andExpect(status().isNotFound());
    }
}