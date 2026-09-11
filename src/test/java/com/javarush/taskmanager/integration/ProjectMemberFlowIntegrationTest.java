package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.project.member.dto.AddMemberRequest;
import com.javarush.taskmanager.project.member.dto.UpdateMemberRoleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectMemberFlowIntegrationTest extends AbstractProjectIntegrationTest {

    private static final String PROJECT_NAME = "Member Test Project";

    @Test
    void addMember_asOwner_returnsCreatedMember() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String newMemberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(newMemberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);

        AddMemberRequest request = new AddMemberRequest(newMemberEmail, ProjectRole.MEMBER);
        mockMvc.perform(post(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(ProjectRole.MEMBER.name()));
    }

    @Test
    void addMember_asMember_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        String memberToken = registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        String outsiderEmail = uniqueEmail(OUTSIDER_PREFIX);
        registerAndLogin(outsiderEmail);
        AddMemberRequest request = new AddMemberRequest(outsiderEmail, ProjectRole.MEMBER);

        mockMvc.perform(post(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMember_grantingOwnerRole_returnsBadRequest() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String newMemberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(newMemberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);

        AddMemberRequest request = new AddMemberRequest(newMemberEmail, ProjectRole.OWNER);
        mockMvc.perform(post(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addMember_alreadyAMember_returnsConflict() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        AddMemberRequest request = new AddMemberRequest(memberEmail, ProjectRole.MEMBER);
        mockMvc.perform(post(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void addMember_unknownEmail_returnsNotFound() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);

        AddMemberRequest request = new AddMemberRequest("nobody@example.com", ProjectRole.MEMBER);
        mockMvc.perform(post(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllMembers_asMember_returnsList() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        String memberToken = registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        mockMvc.perform(get(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllMembers_asOutsider_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String outsiderToken = registerAndLogin(uniqueEmail(OUTSIDER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);

        mockMvc.perform(get(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMemberRole_asOwner_returnsUpdatedRole() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        String memberId = addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        UpdateMemberRoleRequest updateRequest = new UpdateMemberRoleRequest(ProjectRole.MANAGER);
        mockMvc.perform(patch(projectMemberPath(projectId, memberId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(ProjectRole.MANAGER.name()));
    }

    @Test
    void updateMemberRole_asManager_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String managerEmail = uniqueEmail(MANAGER_PREFIX);
        String managerToken = registerAndLogin(managerEmail);
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, managerEmail, ProjectRole.MANAGER);
        String memberId = addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        UpdateMemberRoleRequest updateRequest = new UpdateMemberRoleRequest(ProjectRole.MANAGER);
        mockMvc.perform(patch(projectMemberPath(projectId, memberId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMemberRole_attemptOwnershipTransfer_returnsBadRequest() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        String memberId = addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        UpdateMemberRoleRequest updateRequest = new UpdateMemberRoleRequest(ProjectRole.OWNER);
        mockMvc.perform(patch(projectMemberPath(projectId, memberId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMemberRole_targetingOwner_returnsBadRequest() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);

        String ownerMemberId = findMemberIdByRole(ownerToken, projectId, ProjectRole.OWNER);

        UpdateMemberRoleRequest updateRequest = new UpdateMemberRoleRequest(ProjectRole.MANAGER);
        mockMvc.perform(patch(projectMemberPath(projectId, ownerMemberId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeMember_asManager_returnsNoContent() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String managerEmail = uniqueEmail(MANAGER_PREFIX);
        String managerToken = registerAndLogin(managerEmail);
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        registerAndLogin(memberEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, managerEmail, ProjectRole.MANAGER);
        String memberId = addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        mockMvc.perform(delete(projectMemberPath(projectId, memberId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeMember_asMember_returnsForbidden() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        String memberToken = registerAndLogin(memberEmail);
        String otherEmail = uniqueEmail(OTHER_PREFIX);
        registerAndLogin(otherEmail);
        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);
        String otherId = addMember(ownerToken, projectId, otherEmail, ProjectRole.MEMBER);

        mockMvc.perform(delete(projectMemberPath(projectId, otherId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeMember_projectOwner_returnsBadRequest() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String projectId = createProject(ownerToken, PROJECT_NAME, null);

        String ownerMemberId = findMemberIdByRole(ownerToken, projectId, ProjectRole.OWNER);

        mockMvc.perform(delete(projectMemberPath(projectId, ownerMemberId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isBadRequest());
    }
}