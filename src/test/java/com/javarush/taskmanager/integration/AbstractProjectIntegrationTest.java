package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.project.member.dto.AddMemberRequest;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractProjectIntegrationTest extends AbstractAuthenticatedIntegrationTest {

    protected static final String PROJECTS_PATH = "/api/projects";
    protected static final String TASKS_PATH = "/api/tasks";

    protected static final String OWNER_PREFIX = "owner";
    protected static final String MEMBER_PREFIX = "member";
    protected static final String MANAGER_PREFIX = "manager";
    protected static final String OTHER_PREFIX = "other";
    protected static final String OUTSIDER_PREFIX = "outsider";

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String projectPath(String projectId) {
        return PROJECTS_PATH + "/" + projectId;
    }

    protected String projectMembersPath(String projectId) {
        return projectPath(projectId) + "/members";
    }

    protected String projectMemberPath(String projectId, String memberId) {
        return projectMembersPath(projectId) + "/" + memberId;
    }

    protected String projectTasksPath(String projectId) {
        return projectPath(projectId) + "/tasks";
    }

    protected String taskPath(String taskId) {
        return TASKS_PATH + "/" + taskId;
    }

    protected String taskAssignSelfPath(String taskId) {
        return taskPath(taskId) + "/assign-self";
    }

    protected String taskStatusPath(String taskId) {
        return taskPath(taskId) + "/status";
    }

    protected String taskCommentsPath(String taskId) {
        return taskPath(taskId) + "/comments";
    }

    protected String taskCommentPath(String taskId, String commentId) {
        return taskCommentsPath(taskId) + "/" + commentId;
    }

    protected String extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return jsonMapper.readTree(body).get("id").asString();
    }

    protected String createProject(String ownerToken, String name, String description) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(name, description);

        MvcResult result = mockMvc.perform(post(PROJECTS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    protected String addMember(String ownerToken, String projectId, String memberEmail, ProjectRole role) throws Exception {
        AddMemberRequest request = new AddMemberRequest(memberEmail, role);

        MvcResult result = mockMvc.perform(post(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    protected String createTask(String token, String projectId, String title) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(title, null, null, null, null);

        MvcResult result = mockMvc.perform(post(projectTasksPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    protected String createComment(String token, String taskId, String text) throws Exception {
        CreateCommentRequest request = new CreateCommentRequest(text);

        MvcResult result = mockMvc.perform(post(taskCommentsPath(taskId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractId(result);
    }

    protected String findMemberIdByRole(String token, String projectId, ProjectRole role) throws Exception {
        String body = mockMvc.perform(get(projectMembersPath(projectId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var membersNode = jsonMapper.readTree(body);
        for (var member : membersNode) {
            if (role.name().equals(member.get("role").asString())) {
                return member.get("id").asString();
            }
        }
        throw new IllegalStateException("No member with role " + role + " found in project " + projectId);
    }
}