package com.device.management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DeviceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should create and retrieve device successfully end-to-end")
    void shouldCreateAndRetrieveDevice_endToEnd() throws Exception {
        // Create
        mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                    {
                      "name": "Pixel",
                      "brand": "Google",
                      "state": "AVAILABLE"
                    }
                """))
                .andExpect(status().isCreated());

        // Verify it exists in the database
        mockMvc.perform(get("/api/v1/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Pixel"));
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void shouldReturn400_whenInvalidRequest() throws Exception {

        mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                {
                  "name": "",
                  "brand": "",
                  "state": null
                }
            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when device not found")
    void shouldReturn404_whenDeviceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/device/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEVICE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return 409 when deleting IN_USE device")
    void shouldReturn409_whenDeletingInUseDevice() throws Exception {
        // Create then set to IN_USE
        String response = mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                        {"name": "Test", "brand": "Brand", "state": "IN_USE"}
                    """))
                .andReturn().getResponse().getContentAsString();

        Long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/device/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_IN_USE"));
    }

    @Test
    @DisplayName("Should return 400 when both brand and state filters are used")
    void shouldReturn400_whenBothFiltersUsed() throws Exception {
        mockMvc.perform(get("/api/v1/device?brand=Apple&state=AVAILABLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILTER_CONFLICT"));
    }

    @Test
    @DisplayName("Should return 400 when invalid state value is provided")
    void shouldReturn400_whenInvalidStateValue() throws Exception {
        mockMvc.perform(post("/api/v1/device")
                        .contentType("application/json")
                        .content("""
                        {"name": "Test", "brand": "Brand", "state": "BROKEN"}
                    """))
                .andExpect(status().isBadRequest());
    }
}