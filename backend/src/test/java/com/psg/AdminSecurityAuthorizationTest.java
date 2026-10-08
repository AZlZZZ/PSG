package com.psg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psg.repository.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    @Test
    @DisplayName("Test 9 — Normal firm user cannot access organization management (403 Forbidden)")
    void testFirmUserCannotAccessOrganizations() throws Exception {
        String token = login("firm1", "firm123");
        mockMvc.perform(get("/api/admin/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 10 — Normal firm user cannot access firm-user management (403 Forbidden)")
    void testFirmUserCannotAccessFirmUsers() throws Exception {
        String token = login("firm1", "firm123");
        mockMvc.perform(get("/api/admin/firm-users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN user can access admin organizations (200 OK)")
    void testAdminCanAccessOrganizations() throws Exception {
        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN can retrieve one organization")
    void testAdminCanRetrieveOrganization() throws Exception {
        String token = login("admin", "admin123");
        var org = organizationRepository.findByName("Telecom Solutions Inc.").orElseThrow();
        mockMvc.perform(get("/api/admin/organizations/" + org.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.getId()))
                .andExpect(jsonPath("$.name").value("Telecom Solutions Inc."))
                .andExpect(jsonPath("$.sectorName").exists());
    }
}
