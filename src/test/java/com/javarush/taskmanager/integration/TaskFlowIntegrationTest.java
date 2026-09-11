package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.project.Project;
import com.javarush.taskmanager.project.ProjectRepository;
import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.member.ProjectMember;
import com.javarush.taskmanager.project.member.ProjectMemberRepository;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import com.javarush.taskmanager.task.dto.UpdateTaskRequest;
import com.javarush.taskmanager.task.dto.UpdateTaskStatusRequest;
import com.javarush.taskmanager.task.TaskStatus;
import com.javarush.taskmanager.user.User;
import com.javarush.taskmanager.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskFlowIntegrationTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private record ProjectAndTokens(UUID projectId, String ownerToken, String memberToken, ProjectMember member) {}

    private ProjectAndTokens setUpProjectWithMember(String ownerEmail, String memberEmail) throws Exception {
        String ownerToken = registerAndLogin(ownerEmail);
        String memberToken = registerAndLogin(memberEmail);

        CreateProjectRequest request = new CreateProjectRequest("Task Test Project", null);
        String responseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(jsonMapper.readTree(responseBody).get("id").asString());

        User memberUser = userRepository.findByEmail(memberEmail).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();
        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .build();
        projectMemberRepository.save(member);

        return new ProjectAndTokens(projectId, ownerToken, memberToken, member);
    }

    @Test
    void createTask_asOwner_returnsCreatedTask() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner4@example.com", "member4@example.com");
        CreateTaskRequest request = new CreateTaskRequest("Implement feature", "Desc", null, null, null);

        mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Implement feature"))
                .andExpect(jsonPath("$.status").value("TO_DO"));
    }

    @Test
    void createTask_asMember_returnsForbidden() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner5@example.com", "member5@example.com");
        CreateTaskRequest request = new CreateTaskRequest("Implement feature", "Desc", null, null, null);

        mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignSelf_asMember_succeeds() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner6@example.com", "member6@example.com");
        CreateTaskRequest createRequest = new CreateTaskRequest("Fix bug", null, null, null, null);

        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        mockMvc.perform(patch("/api/tasks/" + taskId + "/assign-self")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(ctx.member().getId().toString()));
    }

    @Test
    void updateStatus_asUnassignedMember_returnsForbidden() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner7@example.com", "member7@example.com");
        CreateTaskRequest createRequest = new CreateTaskRequest("Untouched task", null, null, null, null);

        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        UpdateTaskStatusRequest statusRequest = new UpdateTaskStatusRequest(TaskStatus.DONE);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTask_asOwner_returnsUpdatedTask() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner15@example.com", "member15@example.com");
        CreateTaskRequest createRequest = new CreateTaskRequest("Old title", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        UpdateTaskRequest updateRequest = new UpdateTaskRequest("New title", null, null, null, null);
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));
    }

    @Test
    void updateTask_asMember_returnsForbidden() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner16@example.com", "member16@example.com");
        CreateTaskRequest createRequest = new CreateTaskRequest("Old title", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        UpdateTaskRequest updateRequest = new UpdateTaskRequest("Hacked title", null, null, null, null);
        mockMvc.perform(patch("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTask_asOwner_returnsNoContent() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner17@example.com", "member17@example.com");
        CreateTaskRequest createRequest = new CreateTaskRequest("To be deleted", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_asMember_returnsForbidden() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner18@example.com", "member18@example.com");
        CreateTaskRequest createRequest = new CreateTaskRequest("Should stay", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProject_withTasksAndComments_cascadesSuccessfully() throws Exception {
        ProjectAndTokens ctx = setUpProjectWithMember("owner23@example.com", "member23@example.com");
        CreateTaskRequest taskRequest = new CreateTaskRequest("Task to cascade", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + ctx.projectId() + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        CreateCommentRequest commentRequest = new CreateCommentRequest("Will be cascaded away");
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(commentRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/projects/" + ctx.projectId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + taskId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken()))
                .andExpect(status().isNotFound());
    }
}