package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.comment.dto.UpdateCommentRequest;
import com.javarush.taskmanager.project.member.ProjectRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentFlowIntegrationTest extends AbstractProjectIntegrationTest {

    private static final String PROJECT_NAME = "Comment Test Project";
    private static final String TASK_TITLE = "Discuss approach";

    private record TaskContext(String taskId, String ownerToken, String memberToken) {}

    private TaskContext setUpTaskWithMember() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        String memberToken = registerAndLogin(memberEmail);

        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);
        String taskId = createTask(ownerToken, projectId, TASK_TITLE);

        return new TaskContext(taskId, ownerToken, memberToken);
    }

    private record TwoMemberTaskContext(String taskId, String ownerToken, String memberToken, String otherMemberToken) {}

    private TwoMemberTaskContext setUpTaskWithTwoMembers() throws Exception {
        String ownerToken = registerAndLogin(uniqueEmail(OWNER_PREFIX));
        String memberEmail = uniqueEmail(MEMBER_PREFIX);
        String otherEmail = uniqueEmail(OTHER_PREFIX);
        String memberToken = registerAndLogin(memberEmail);
        String otherMemberToken = registerAndLogin(otherEmail);

        String projectId = createProject(ownerToken, PROJECT_NAME, null);
        addMember(ownerToken, projectId, memberEmail, ProjectRole.MEMBER);
        addMember(ownerToken, projectId, otherEmail, ProjectRole.MEMBER);
        String taskId = createTask(ownerToken, projectId, TASK_TITLE);

        return new TwoMemberTaskContext(taskId, ownerToken, memberToken, otherMemberToken);
    }

    @Test
    void addComment_asMember_returnsCreatedComment() throws Exception {
        TaskContext ctx = setUpTaskWithMember();
        CreateCommentRequest request = new CreateCommentRequest("Looks reasonable to me");

        mockMvc.perform(post(taskCommentsPath(ctx.taskId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Looks reasonable to me"));
    }

    @Test
    void addComment_noToken_returnsUnauthorized() throws Exception {
        TaskContext ctx = setUpTaskWithMember();
        CreateCommentRequest request = new CreateCommentRequest("Should not work");

        mockMvc.perform(post(taskCommentsPath(ctx.taskId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getComments_afterAdding_returnsComment() throws Exception {
        TaskContext ctx = setUpTaskWithMember();
        createComment(ctx.memberToken(), ctx.taskId(), "First comment");

        mockMvc.perform(get(taskCommentsPath(ctx.taskId()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("First comment"));
    }

    @Test
    void updateComment_asAuthor_returnsUpdatedComment() throws Exception {
        TaskContext ctx = setUpTaskWithMember();
        String commentId = createComment(ctx.memberToken(), ctx.taskId(), "Original text");

        UpdateCommentRequest updateRequest = new UpdateCommentRequest("Updated text");
        mockMvc.perform(patch(taskCommentPath(ctx.taskId(), commentId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    void updateComment_asUnrelatedMember_returnsForbidden() throws Exception {
        TwoMemberTaskContext ctx = setUpTaskWithTwoMembers();
        String commentId = createComment(ctx.memberToken(), ctx.taskId(), "Original text");

        UpdateCommentRequest updateRequest = new UpdateCommentRequest("Should not work");
        mockMvc.perform(patch(taskCommentPath(ctx.taskId(), commentId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.otherMemberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_asAuthor_returnsNoContent() throws Exception {
        TaskContext ctx = setUpTaskWithMember();
        String commentId = createComment(ctx.memberToken(), ctx.taskId(), "To be deleted");

        mockMvc.perform(delete(taskCommentPath(ctx.taskId(), commentId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.memberToken())))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_asProjectOwnerModerating_returnsNoContent() throws Exception {
        TaskContext ctx = setUpTaskWithMember();
        String commentId = createComment(ctx.memberToken(), ctx.taskId(), "To be moderated");

        mockMvc.perform(delete(taskCommentPath(ctx.taskId(), commentId))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ctx.ownerToken())))
                .andExpect(status().isNoContent());
    }
}