package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectFlowIntegrationTest extends AbstractAuthenticatedIntegrationTest {

    @Test
    void createProject_authenticatedUser_returnsCreatedProject() throws Exception {
        String token = registerAndLogin("owner1@example.com");
        CreateProjectRequest request = new CreateProjectRequest("My Project", "Description");

        mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Project"));
    }

    @Test
    void createProject_noToken_returnsUnauthorized() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("My Project", "Description");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyProjects_afterCreating_returnsOwnedProject() throws Exception {
        String token = registerAndLogin("owner2@example.com");
        CreateProjectRequest request = new CreateProjectRequest("Second Project", null);

        mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Second Project"));
    }

    @Test
    void getProject_notAMember_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin("owner3@example.com");
        String outsiderToken = registerAndLogin("outsider@example.com");
        CreateProjectRequest request = new CreateProjectRequest("Private Project", null);

        String responseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String projectId = jsonMapper.readTree(responseBody).get("id").asString();

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }
}