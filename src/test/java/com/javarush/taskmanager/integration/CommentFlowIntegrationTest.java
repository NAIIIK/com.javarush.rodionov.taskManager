package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.comment.dto.UpdateCommentRequest;
import com.javarush.taskmanager.project.Project;
import com.javarush.taskmanager.project.ProjectRepository;
import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.member.ProjectMember;
import com.javarush.taskmanager.project.member.ProjectMemberRepository;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
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

class CommentFlowIntegrationTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private record TaskContext(String taskId, String ownerToken, String memberToken) {}

    private TaskContext setUpTaskWithMember(String ownerEmail, String memberEmail) throws Exception {
        String ownerToken = registerAndLogin(ownerEmail);
        String memberToken = registerAndLogin(memberEmail);

        CreateProjectRequest projectRequest = new CreateProjectRequest("Comment Test Project", null);
        String projectResponseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(jsonMapper.readTree(projectResponseBody).get("id").asString());

        User memberUser = userRepository.findByEmail(memberEmail).orElseThrow();
        Project project = projectRepository.findById(projectId).orElseThrow();
        projectMemberRepository.save(ProjectMember.builder()
                .project(project)
                .user(memberUser)
                .role(ProjectRole.MEMBER)
                .build());

        CreateTaskRequest taskRequest = new CreateTaskRequest("Discuss approach", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        return new TaskContext(taskId, ownerToken, memberToken);
    }

    @Test
    void addComment_asMember_returnsCreatedComment() throws Exception {
        TaskContext ctx = setUpTaskWithMember("owner8@example.com", "member8@example.com");
        CreateCommentRequest request = new CreateCommentRequest("Looks reasonable to me");

        mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Looks reasonable to me"));
    }

    @Test
    void addComment_noToken_returnsUnauthorized() throws Exception {
        TaskContext ctx = setUpTaskWithMember("owner9@example.com", "member9@example.com");
        CreateCommentRequest request = new CreateCommentRequest("Should not work");

        mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getComments_afterAdding_returnsComment() throws Exception {
        TaskContext ctx = setUpTaskWithMember("owner10@example.com", "member10@example.com");
        CreateCommentRequest request = new CreateCommentRequest("First comment");

        mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("First comment"));
    }

    private record TwoMemberTaskContext(String taskId, String ownerToken, String memberToken, String otherMemberToken) {}

    private TwoMemberTaskContext setUpTaskWithTwoMembers(String ownerEmail, String memberEmail, String otherMemberEmail) throws Exception {
        String ownerToken = registerAndLogin(ownerEmail);
        String memberToken = registerAndLogin(memberEmail);
        String otherMemberToken = registerAndLogin(otherMemberEmail);

        CreateProjectRequest projectRequest = new CreateProjectRequest("Comment Test Project", null);
        String projectResponseBody = mockMvc.perform(post("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID projectId = UUID.fromString(jsonMapper.readTree(projectResponseBody).get("id").asString());

        Project project = projectRepository.findById(projectId).orElseThrow();
        User memberUser = userRepository.findByEmail(memberEmail).orElseThrow();
        User otherMemberUser = userRepository.findByEmail(otherMemberEmail).orElseThrow();
        projectMemberRepository.save(ProjectMember.builder().project(project).user(memberUser).role(ProjectRole.MEMBER).build());
        projectMemberRepository.save(ProjectMember.builder().project(project).user(otherMemberUser).role(ProjectRole.MEMBER).build());

        CreateTaskRequest taskRequest = new CreateTaskRequest("Discuss approach", null, null, null, null);
        String taskResponseBody = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskId = jsonMapper.readTree(taskResponseBody).get("id").asString();

        return new TwoMemberTaskContext(taskId, ownerToken, memberToken, otherMemberToken);
    }

    @Test
    void updateComment_asAuthor_returnsUpdatedComment() throws Exception {
        TaskContext ctx = setUpTaskWithMember("owner11@example.com", "member11@example.com");
        CreateCommentRequest createRequest = new CreateCommentRequest("Original text");
        String commentResponseBody = mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commentId = jsonMapper.readTree(commentResponseBody).get("id").asString();

        UpdateCommentRequest updateRequest = new UpdateCommentRequest("Updated text");
        mockMvc.perform(patch("/api/tasks/" + ctx.taskId() + "/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    void updateComment_asUnrelatedMember_returnsForbidden() throws Exception {
        TwoMemberTaskContext ctx = setUpTaskWithTwoMembers("owner12@example.com", "member12@example.com", "other12@example.com");
        CreateCommentRequest createRequest = new CreateCommentRequest("Original text");
        String commentResponseBody = mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commentId = jsonMapper.readTree(commentResponseBody).get("id").asString();

        UpdateCommentRequest updateRequest = new UpdateCommentRequest("Should not work");
        mockMvc.perform(patch("/api/tasks/" + ctx.taskId() + "/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.otherMemberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_asAuthor_returnsNoContent() throws Exception {
        TaskContext ctx = setUpTaskWithMember("owner13@example.com", "member13@example.com");
        CreateCommentRequest createRequest = new CreateCommentRequest("To be deleted");
        String commentResponseBody = mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commentId = jsonMapper.readTree(commentResponseBody).get("id").asString();

        mockMvc.perform(delete("/api/tasks/" + ctx.taskId() + "/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_asProjectOwnerModerating_returnsNoContent() throws Exception {
        TaskContext ctx = setUpTaskWithMember("owner14@example.com", "member14@example.com");
        CreateCommentRequest createRequest = new CreateCommentRequest("To be moderated");
        String commentResponseBody = mockMvc.perform(post("/api/tasks/" + ctx.taskId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commentId = jsonMapper.readTree(commentResponseBody).get("id").asString();

        mockMvc.perform(delete("/api/tasks/" + ctx.taskId() + "/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ctx.ownerToken()))
                .andExpect(status().isNoContent());
    }
}