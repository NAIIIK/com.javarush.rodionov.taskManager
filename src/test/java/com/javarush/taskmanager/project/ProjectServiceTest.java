package com.javarush.taskmanager.project;

import com.javarush.taskmanager.exception.ResourceNotFoundException;
import com.javarush.taskmanager.project.dto.CreateProjectRequest;
import com.javarush.taskmanager.project.dto.ProjectResponse;
import com.javarush.taskmanager.project.member.ProjectAccessGuard;
import com.javarush.taskmanager.project.member.ProjectMemberRepository;
import com.javarush.taskmanager.project.member.ProjectRole;
import com.javarush.taskmanager.user.GlobalRole;
import com.javarush.taskmanager.user.User;
import com.javarush.taskmanager.user.UserRepository;
import java.util.Optional;
import java.util.UUID;

import com.javarush.taskmanager.util.ExceptionMessages;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String PROJECT_NAME = "New Project";
    private static final String PROJECT_DESCRIPTION = "Description";
    private static final String EXISTING_PROJECT_NAME = "Existing";

    private final UUID projectId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(ownerId)
                .email(OWNER_EMAIL)
                .globalRole(GlobalRole.USER)
                .build();

        project = Project.builder()
                .id(projectId)
                .name(EXISTING_PROJECT_NAME)
                .build();
    }

    @Test
    void createProject_ownerExists_createsProjectAndOwnerMembership() {
        CreateProjectRequest request = new CreateProjectRequest(PROJECT_NAME, PROJECT_DESCRIPTION);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(projectMapper.toResponse(any(Project.class)))
                .thenReturn(createProjectResponse(projectId, PROJECT_NAME, PROJECT_DESCRIPTION, ownerId));

        ProjectResponse response = projectService.createProject(ownerId, request);

        assertThat(response.name()).isEqualTo(PROJECT_NAME);
        verify(projectRepository).save(argThat(p -> p.getOwner().equals(owner) && p.getName().equals(PROJECT_NAME)));
        verify(projectMemberRepository).save(argThat(m -> m.getRole() == ProjectRole.OWNER && m.getUser().equals(owner)));
    }

    @Test
    void createProject_ownerNotFound_throwsResourceNotFound() {
        CreateProjectRequest request = new CreateProjectRequest(PROJECT_NAME, PROJECT_DESCRIPTION);
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(ownerId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProject_memberRequesting_returnsProject() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project))
                .thenReturn(createProjectResponse(projectId, EXISTING_PROJECT_NAME, null, null));

        ProjectResponse response = projectService.getProject(projectId, requesterId);

        assertThat(response.id()).isEqualTo(projectId);
        verify(projectAccessGuard).requireMembership(projectId, requesterId);
    }

    @Test
    void getProject_notAMember_throwsAccessDenied() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException(ExceptionMessages.NOT_A_MEMBER_MSG))
                .when(projectAccessGuard).requireMembership(projectId, requesterId);

        assertThatThrownBy(() -> projectService.getProject(projectId, requesterId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getProject_notFound_throwsResourceNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(projectId, requesterId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ProjectResponse createProjectResponse(UUID id, String name, String description, UUID ownerId) {
        return new ProjectResponse(id, name, description, ownerId, null);
    }
}