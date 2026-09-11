package com.javarush.taskmanager.task;

import com.javarush.taskmanager.exception.ResourceNotFoundException;
import com.javarush.taskmanager.project.Project;
import com.javarush.taskmanager.project.ProjectRepository;
import com.javarush.taskmanager.project.member.ProjectAccessGuard;
import com.javarush.taskmanager.project.member.ProjectMember;
import com.javarush.taskmanager.project.member.ProjectMemberRepository;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import com.javarush.taskmanager.task.dto.TaskResponse;
import com.javarush.taskmanager.task.dto.UpdateTaskStatusRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private static final String NOT_A_MEMBER_MSG = "You are not a member of this project";

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskMapper taskMapper;

    public TaskResponse createTask(UUID projectId, UUID requesterId, CreateTaskRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        projectAccessGuard.requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        ProjectMember assignee = null;
        if (request.assigneeId() != null) {
            assignee = projectMemberRepository.findById(request.assigneeId())
                    .filter(member -> member.getProject().getId().equals(projectId))
                    .orElseThrow(() -> new IllegalArgumentException("Assignee is not a member of this project"));
        }

        Task task = Task.builder()
                .project(project)
                .title(request.title())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .assignee(assignee)
                .dueDate(request.dueDate())
                .build();
        taskRepository.save(task);

        return taskMapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksForProject(UUID projectId, UUID requesterId) {
        projectAccessGuard.requireMembership(projectId, requesterId);
        return taskRepository.findAllByProjectId(projectId).stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public TaskResponse assignSelf(UUID taskId, UUID requesterId) {
        Task task = getTaskOrThrow(taskId);
        UUID projectId = task.getProject().getId();
        projectAccessGuard.requireMembership(projectId, requesterId);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId)
                .orElseThrow(() -> new AccessDeniedException(NOT_A_MEMBER_MSG));

        task.setAssignee(member);
        return taskMapper.toResponse(task);
    }

    public TaskResponse updateStatus(UUID taskId, UUID requesterId, UpdateTaskStatusRequest request) {
        Task task = getTaskOrThrow(taskId);
        UUID projectId = task.getProject().getId();

        ProjectMember requester = projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId)
                .orElseThrow(() -> new AccessDeniedException(NOT_A_MEMBER_MSG));

        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(requester.getId());
        boolean isManagerOrOwner = requester.getRole().getWeight() <= ProjectRole.MANAGER.getWeight();

        if (!isAssignee && !isManagerOrOwner) {
            throw new AccessDeniedException("Only the assignee or a manager/owner can change task status");
        }

        task.setStatus(request.status());
        return taskMapper.toResponse(task);
    }

    private Task getTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }
}