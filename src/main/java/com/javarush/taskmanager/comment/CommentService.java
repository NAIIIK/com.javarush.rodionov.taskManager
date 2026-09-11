package com.javarush.taskmanager.comment;

import com.javarush.taskmanager.comment.dto.CommentResponse;
import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.exception.ResourceNotFoundException;
import com.javarush.taskmanager.project.member.ProjectAccessGuard;
import com.javarush.taskmanager.project.member.ProjectMember;
import com.javarush.taskmanager.project.member.ProjectMemberRepository;
import com.javarush.taskmanager.task.Task;
import com.javarush.taskmanager.task.TaskRepository;
import java.util.List;
import java.util.UUID;

import com.javarush.taskmanager.util.ExceptionMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final CommentMapper commentMapper;

    public CommentResponse addComment(UUID taskId, UUID requesterId, CreateCommentRequest request) {
        Task task = getTaskOrThrow(taskId);
        UUID projectId = task.getProject().getId();
        projectAccessGuard.requireMembership(projectId, requesterId);

        ProjectMember author = projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId)
                .orElseThrow(() -> new AccessDeniedException(ExceptionMessages.NOT_A_MEMBER_MSG));

        Comment comment = Comment.builder()
                .task(task)
                .author(author)
                .text(request.text())
                .build();
        commentRepository.save(comment);

        return commentMapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID taskId, UUID requesterId) {
        Task task = getTaskOrThrow(taskId);
        projectAccessGuard.requireMembership(task.getProject().getId(), requesterId);

        return commentRepository.findAllByTaskId(taskId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    private Task getTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionMessages.TASK_NOT_FOUND_MSG + taskId));
    }
}