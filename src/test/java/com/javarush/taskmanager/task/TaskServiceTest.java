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
import org.junit.jupiter.api.BeforeEach;
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
class TaskServiceTest {

    private static final String TASK_TITLE = "Title";
    private static final String TASK_DESC = "Description";

    private final UUID projectId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private final UUID assigneeMemberId = UUID.randomUUID();
    private final UUID otherMemberId = UUID.randomUUID();

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

    private Project project;
    private ProjectMember assigneeMember;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .id(projectId)
                .name("Project")
                .build();

        assigneeMember = ProjectMember.builder()
                .id(assigneeMemberId)
                .role(ProjectRole.MEMBER)
                .build();
    }

    @Test
    void createTask_requesterIsManager_createsTask() {
        CreateTaskRequest request = new CreateTaskRequest(TASK_TITLE, TASK_DESC, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskMapper.toResponse(any(Task.class)))
                .thenReturn(createTaskResponse(taskId, TASK_DESC, TaskStatus.TO_DO, null));

        TaskResponse response = taskService.createTask(projectId, requesterId, request);

        assertThat(response.title()).isEqualTo(TASK_TITLE);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void createTask_requesterIsMember_throwsAccessDenied() {
        CreateTaskRequest request = new CreateTaskRequest(TASK_TITLE, TASK_DESC, null, null, null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException("Requires role MANAGER or higher"))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        assertThatThrownBy(() -> taskService.createTask(projectId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_requesterIsAssignee_updatesStatus() {
        Task task = createTestTask(assigneeMember);
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(assigneeMember));
        when(taskMapper.toResponse(task))
                .thenReturn(createTaskResponse(taskId, null, TaskStatus.IN_PROGRESS, assigneeMemberId));

        TaskResponse response = taskService.updateStatus(taskId, requesterId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_requesterIsUnrelatedMember_throwsAccessDenied() {
        ProjectMember requester = ProjectMember.builder().id(otherMemberId).role(ProjectRole.MEMBER).build();
        Task task = createTestTask(assigneeMember);
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> taskService.updateStatus(taskId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_requesterIsManager_updatesStatusEvenWithoutAssignment() {
        ProjectMember manager = ProjectMember.builder().id(otherMemberId).role(ProjectRole.MANAGER).build();
        Task task = createTestTask(null);
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, requesterId))
                .thenReturn(Optional.of(manager));
        when(taskMapper.toResponse(task))
                .thenReturn(createTaskResponse(taskId, null, TaskStatus.DONE, null));

        TaskResponse response = taskService.updateStatus(taskId, requesterId, request);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void getTaskOrThrow_taskNotFound_throwsResourceNotFound() {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(taskId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Task createTestTask(ProjectMember assignee) {
        return Task.builder()
                .id(taskId)
                .project(project)
                .status(TaskStatus.TO_DO)
                .assignee(assignee)
                .build();
    }

    private TaskResponse createTaskResponse(UUID id, String description, TaskStatus status, UUID assigneeId) {
        return new TaskResponse(id, projectId, TASK_TITLE, description, status, TaskPriority.MEDIUM, assigneeId, null, null);
    }
}