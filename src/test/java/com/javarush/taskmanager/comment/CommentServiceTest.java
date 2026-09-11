package com.javarush.taskmanager.comment;

import com.javarush.taskmanager.comment.dto.CommentResponse;
import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.comment.dto.UpdateCommentRequest;
import com.javarush.taskmanager.exception.ResourceNotFoundException;
import com.javarush.taskmanager.project.Project;
import com.javarush.taskmanager.project.member.ProjectAccessGuard;
import com.javarush.taskmanager.project.member.ProjectMember;
import com.javarush.taskmanager.project.member.ProjectMemberRepository;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.task.Task;
import com.javarush.taskmanager.task.TaskRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.javarush.taskmanager.util.ExceptionMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final String COMMENT_MSG = "Looks good";
    private static final String UPDATED_TEXT = "Updated text";

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    @Test
    void addComment_requesterIsMember_savesComment() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember author = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        CreateCommentRequest request = new CreateCommentRequest(COMMENT_MSG);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(author));
        when(commentMapper.toResponse(any(Comment.class)))
                .thenReturn(new CommentResponse(UUID.randomUUID(), taskId, author.getId(), "Looks good", null));

        CommentResponse response = commentService.addComment(taskId, requesterId, request);

        assertThat(response.text()).isEqualTo(COMMENT_MSG);
        assertThat(response.authorId()).isEqualTo(author.getId());
    }

    @Test
    void addComment_requesterNotAMember_throwsAccessDenied() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        CreateCommentRequest request = new CreateCommentRequest(COMMENT_MSG);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new AccessDeniedException(ExceptionMessages.NOT_A_MEMBER_MSG))
                .when(projectAccessGuard).requireMembership(projectId, requesterId);

        assertThatThrownBy(() -> commentService.addComment(taskId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addComment_taskNotFound_throwsResourceNotFound() {
        UUID taskId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(COMMENT_MSG);
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(taskId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getComments_requesterIsMember_returnsComments() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        Comment comment = Comment.builder().id(UUID.randomUUID()).task(task).text(COMMENT_MSG).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(commentRepository.findAllByTaskId(taskId)).thenReturn(List.of(comment));
        when(commentMapper.toResponse(comment))
                .thenReturn(new CommentResponse(comment.getId(), taskId, null, COMMENT_MSG, null));

        List<CommentResponse> responses = commentService.getComments(taskId, requesterId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().text()).isEqualTo(COMMENT_MSG);
    }

    @Test
    void updateComment_authorEditsOwnComment_updatesText() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember author = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        Comment comment = Comment.builder().id(commentId).task(task).author(author).text(COMMENT_MSG).build();
        UpdateCommentRequest request = new UpdateCommentRequest(UPDATED_TEXT);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(author));
        when(commentRepository.findByIdAndTaskId(commentId, taskId)).thenReturn(Optional.of(comment));
        when(commentMapper.toResponse(comment))
                .thenReturn(new CommentResponse(commentId, taskId, author.getId(), UPDATED_TEXT, null));

        CommentResponse response = commentService.updateComment(taskId, commentId, requesterId, request);

        assertThat(response.text()).isEqualTo(UPDATED_TEXT);
        assertThat(comment.getText()).isEqualTo(UPDATED_TEXT);
    }

    @Test
    void updateComment_managerEditsOthersComment_updatesText() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember author = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        ProjectMember manager = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MANAGER).build();
        Comment comment = Comment.builder().id(commentId).task(task).author(author).text(COMMENT_MSG).build();
        UpdateCommentRequest request = new UpdateCommentRequest(UPDATED_TEXT);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(manager));
        when(commentRepository.findByIdAndTaskId(commentId, taskId)).thenReturn(Optional.of(comment));
        when(commentMapper.toResponse(comment))
                .thenReturn(new CommentResponse(commentId, taskId, author.getId(), UPDATED_TEXT, null));

        CommentResponse response = commentService.updateComment(taskId, commentId, requesterId, request);

        assertThat(response.text()).isEqualTo(UPDATED_TEXT);
    }

    @Test
    void updateComment_memberEditsOthersComment_throwsAccessDenied() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember author = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        ProjectMember requester = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        Comment comment = Comment.builder().id(commentId).task(task).author(author).text(COMMENT_MSG).build();
        UpdateCommentRequest request = new UpdateCommentRequest(UPDATED_TEXT);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(requester));
        when(commentRepository.findByIdAndTaskId(commentId, taskId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(taskId, commentId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateComment_commentNotFound_throwsResourceNotFound() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember requester = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        UpdateCommentRequest request = new UpdateCommentRequest(UPDATED_TEXT);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(requester));
        when(commentRepository.findByIdAndTaskId(commentId, taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(taskId, commentId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteComment_authorDeletesOwnComment_deletesIt() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember author = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        Comment comment = Comment.builder().id(commentId).task(task).author(author).text(COMMENT_MSG).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(author));
        when(commentRepository.findByIdAndTaskId(commentId, taskId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(taskId, commentId, requesterId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_memberDeletesOthersComment_throwsAccessDenied() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        ProjectMember author = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        ProjectMember requester = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        Comment comment = Comment.builder().id(commentId).task(task).author(author).text(COMMENT_MSG).build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(requester));
        when(commentRepository.findByIdAndTaskId(commentId, taskId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(taskId, commentId, requesterId))
                .isInstanceOf(AccessDeniedException.class);
    }
}