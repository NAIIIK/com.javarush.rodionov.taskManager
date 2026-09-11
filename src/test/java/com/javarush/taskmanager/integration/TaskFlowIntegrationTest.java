package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.task.TaskStatus;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import com.javarush.taskmanager.task.dto.UpdateTaskRequest;
import com.javarush.taskmanager.task.dto.UpdateTaskStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskFlowIntegrationTest extends AbstractProjectIntegrationTest {

    private static final String PROJECT_NAME = "Task Test Project";

    private record ProjectWithMember(String projectId, String ownerToken, String memberToken, String memberId) {}

    private ProjectWithMember setUpProjectWithMember() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        String memberToken = registerAndLogin(memberEmail);

        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        String memberId = addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);

        return new ProjectWithMember(projectId, ownerToken, memberToken, memberId);
    }

    @Test
    void createTask_asOwner_returnsCreatedTask() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        CreateTaskRequest request = new CreateTaskRequest("Implement feature", "Desc", null, null, null);

        mockMvc.perform(post(projectTasksPath(ctx.projectId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Implement feature"))
                .andExpect(jsonPath("$.status").value(TaskStatus.TO_DO.name()));
    }

    @Test
    void createTask_asMember_returnsForbidden() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        CreateTaskRequest request = new CreateTaskRequest("Implement feature", "Desc", null, null, null);

        mockMvc.perform(post(projectTasksPath(ctx.projectId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignSelf_asMember_succeeds() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "Fix bug");

        mockMvc.perform(patch(taskAssignSelfPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(ctx.memberId()));
    }

    @Test
    void updateStatus_asUnassignedMember_returnsForbidden() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "Untouched task");

        UpdateTaskStatusRequest statusRequest = new UpdateTaskStatusRequest(TaskStatus.DONE);
        mockMvc.perform(patch(taskStatusPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTask_asOwner_returnsUpdatedTask() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "Old title");

        UpdateTaskRequest updateRequest = new UpdateTaskRequest("New title", null, null, null, null);
        mockMvc.perform(patch(taskPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));
    }

    @Test
    void updateTask_asMember_returnsForbidden() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "Old title");

        UpdateTaskRequest updateRequest = new UpdateTaskRequest("Hacked title", null, null, null, null);
        mockMvc.perform(patch(taskPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTask_asOwner_returnsNoContent() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "To be deleted");

        mockMvc.perform(delete(taskPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken())))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_asMember_returnsForbidden() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "Should stay");

        mockMvc.perform(delete(taskPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_withTasksAndComments_cascadesSuccessfully() throws Exception {
        ProjectWithMember ctx = setUpProjectWithMember();
        String taskId = createTask(ctx.ownerToken(), ctx.projectId(), "Task to cascade");
        createComment(ctx.memberToken(), taskId, "Will be cascaded away");

        mockMvc.perform(delete(projectPath(ctx.projectId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(taskCommentsPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken())))
                .andExpect(status().isNotFound());
    }
}