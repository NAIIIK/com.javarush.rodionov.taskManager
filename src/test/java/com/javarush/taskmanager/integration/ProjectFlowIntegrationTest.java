package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.dto.UpdateProjectRequest;
import com.javarush.taskmanager.project.member.ProjectRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectFlowIntegrationTest extends AbstractProjectIntegrationTest {

    @Test
    void createProject_authenticatedUser_returnsCreatedProject() throws Exception {
        String token = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        CreateProjectRequest request = new CreateProjectRequest("My Project", "Description");

        mockMvc.perform(post(PROJECTS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Project"));
    }

    @Test
    void createProject_noToken_returnsUnauthorized() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest("My Project", "Description");

        mockMvc.perform(post(PROJECTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyProjects_afterCreating_returnsOwnedProject() throws Exception {
        String token = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        createProject(token, "Second Project", null);

        mockMvc.perform(get(PROJECTS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Second Project"));
    }

    @Test
    void getProject_notAMember_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String outsiderToken = registerAndLogin(uniqueEmail(OUTSIDER_PREFIX));
        String projectId = createProject(ownerToken, "Private Project", null);

        mockMvc.perform(get(projectPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProject_asOwner_returnsUpdatedProject() throws Exception {
        String token = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(token, "Old Name", null);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("New Name", "New description");
        mockMvc.perform(patch(projectPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void updateProject_asManager_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String managerEmail = uniqueEmail(MANAGER_PREFIX);
        String managerToken = registerAndLogin(managerEmail);
        String projectId = createProject(ownerToken, "Team Project", null);
        addMember(ownerToken, projectId, managerEmail, ProjectRole.MANAGER);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("Hacked Name", null);
        mockMvc.perform(patch(projectPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_asOwner_returnsNoContent() throws Exception {
        String token = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(token, "To be deleted", null);

        mockMvc.perform(delete(projectPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_asManager_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String managerEmail = uniqueEmail(MANAGER_PREFIX);
        String managerToken = registerAndLogin(managerEmail);
        String projectId = createProject(ownerToken, "Protected Project", null);
        addMember(ownerToken, projectId, managerEmail, ProjectRole.MANAGER);

        mockMvc.perform(delete(projectPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());
    }
}