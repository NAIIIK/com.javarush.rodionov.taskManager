package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.dto.UpdateProjectRequest;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.project.member.dto.AddMemberRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Test
    void updateProject_asOwner_returnsUpdatedProject() throws Exception {
        String token = registerAndLogin("owner19@example.com");
        CreateProjectRequest createRequest = new CreateProjectRequest("Old Name", null);
        String responseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = jsonMapper.readTree(responseBody).get("id").asString();

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("New Name", "New description");
        mockMvc.perform(patch("/api/projects/" + projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void updateProject_asManager_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin("owner20@example.com");
        String managerToken = registerAndLogin("manager20@example.com");
        CreateProjectRequest createRequest = new CreateProjectRequest("Team Project", null);
        String responseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = jsonMapper.readTree(responseBody).get("id").asString();

        AddMemberRequest addMemberRequest = new AddMemberRequest("manager20@example.com", ProjectRole.MANAGER);
        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isCreated());

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("Hacked Name", null);
        mockMvc.perform(patch("/api/projects/" + projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_asOwner_returnsNoContent() throws Exception {
        String token = registerAndLogin("owner21@example.com");
        CreateProjectRequest createRequest = new CreateProjectRequest("To be deleted", null);
        String responseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = jsonMapper.readTree(responseBody).get("id").asString();

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_asManager_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin("owner22@example.com");
        String managerToken = registerAndLogin("manager22@example.com");
        CreateProjectRequest createRequest = new CreateProjectRequest("Protected Project", null);
        String responseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = jsonMapper.readTree(responseBody).get("id").asString();

        AddMemberRequest addMemberRequest = new AddMemberRequest("manager22@example.com", ProjectRole.MANAGER);
        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(addMemberRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }
}