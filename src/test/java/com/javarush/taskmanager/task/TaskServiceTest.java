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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTask_requesterIsManager_createsTask() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Project").build();
        CreateTaskRequest request = new CreateTaskRequest("Title", "Desc", null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskMapper.toResponse(any(Task.class)))
                .thenReturn(new TaskResponse(UUID.randomUUID(), projectId, "Title", "Desc", TaskStatus.TO_DO, TaskPriority.MEDIUM, null, null, null));

        TaskResponse response = taskService.createTask(projectId, requesterId, request);

        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void createTask_requesterIsMember_throwsAccessDenied() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest("Title", "Desc", null, null, null);

        org.mockito.Mockito.doThrow(new AccessDeniedException("Requires role MANAGER or higher"))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        assertThatThrownBy(() -> taskService.createTask(projectId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_requesterIsAssignee_updatesStatus() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        ProjectMember assignee = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        Task task = Task.builder().id(taskId).project(project).status(TaskStatus.TO_DO).assignee(assignee).build();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(assignee));
        when(taskMapper.toResponse(task))
                .thenReturn(new TaskResponse(taskId, projectId, "Title", null, TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, assignee.getId(), null, null));

        TaskResponse response = taskService.updateStatus(taskId, requesterId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_requesterIsUnrelatedMember_throwsAccessDenied() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        ProjectMember assignee = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        ProjectMember requester = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();
        Task task = Task.builder().id(taskId).project(project).status(TaskStatus.TO_DO).assignee(assignee).build();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_requesterIsManager_updatesStatusEvenWithoutAssignment() {
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).build();
        ProjectMember manager = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MANAGER).build();
        Task task = Task.builder().id(taskId).project(project).status(TaskStatus.TO_DO).assignee(null).build();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(manager));
        when(taskMapper.toResponse(task))
                .thenReturn(new TaskResponse(taskId, projectId, "Title", null, TaskStatus.DONE, TaskPriority.MEDIUM, null, null, null));

        TaskResponse response = taskService.updateStatus(taskId, requesterId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void getTaskOrThrow_taskNotFound_throwsResourceNotFound() {
        UUID taskId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(taskId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}