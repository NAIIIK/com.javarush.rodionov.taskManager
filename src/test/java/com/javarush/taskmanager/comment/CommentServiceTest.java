package com.javarush.taskmanager.comment;

import com.javarush.taskmanager.comment.dto.CommentResponse;
import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

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
        CreateCommentRequest request = new CreateCommentRequest("Looks good");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(author));
        when(commentMapper.toResponse(any(Comment.class)))
                .thenReturn(new CommentResponse(UUID.randomUUID(), taskId, author.getId(), "Looks good", null));

        CommentResponse response = commentService.addComment(taskId, requesterId, request);

        assertThat(response.text()).isEqualTo("Looks good");
        assertThat(response.authorId()).isEqualTo(author.getId());
    }

    @Test
    void addComment_requesterNotAMember_throwsAccessDenied() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        Task task = Task.builder().id(taskId).project(project).build();
        CreateCommentRequest request = new CreateCommentRequest("Looks good");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doThrow(new AccessDeniedException("You are not a member of this project"))
                .when(projectAccessGuard).requireMembership(projectId, requesterId);

        assertThatThrownBy(() -> commentService.addComment(taskId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addComment_taskNotFound_throwsResourceNotFound() {
        UUID taskId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest("Looks good");
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
        Comment comment = Comment.builder().id(UUID.randomUUID()).task(task).text("Hi").build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(commentRepository.findAllByTaskId(taskId)).thenReturn(List.of(comment));
        when(commentMapper.toResponse(comment))
                .thenReturn(new CommentResponse(comment.getId(), taskId, null, "Hi", null));

        List<CommentResponse> responses = commentService.getComments(taskId, requesterId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().text()).isEqualTo("Hi");
    }
}