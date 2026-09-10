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

    @Test
    void createProject_ownerExists_createsProjectAndOwnerMembership() {
        UUID ownerId = UUID.randomUUID();
        User owner = User.builder().id(ownerId).email("owner@example.com").globalRole(GlobalRole.USER).build();
        CreateProjectRequest request = new CreateProjectRequest("New Project", "Description");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(projectMapper.toResponse(any(Project.class)))
                .thenReturn(new ProjectResponse(UUID.randomUUID(), "New Project", "Description", ownerId, null));

        ProjectResponse response = projectService.createProject(ownerId, request);

        assertThat(response.name()).isEqualTo("New Project");
        verify(projectRepository).save(argThat(p -> p.getOwner().equals(owner) && p.getName().equals("New Project")));
        verify(projectMemberRepository).save(argThat(m -> m.getRole() == ProjectRole.OWNER && m.getUser().equals(owner)));
    }

    @Test
    void createProject_ownerNotFound_throwsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest("New Project", "Description");
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(ownerId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProject_memberRequesting_returnsProject() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Existing").build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project))
                .thenReturn(new ProjectResponse(projectId, "Existing", null, null, null));

        ProjectResponse response = projectService.getProject(projectId, requesterId);

        assertThat(response.id()).isEqualTo(projectId);
        verify(projectAccessGuard).requireMembership(projectId, requesterId);
    }

    @Test
    void getProject_notAMember_throwsAccessDenied() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Existing").build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException("You are not a member of this project"))
                .when(projectAccessGuard).requireMembership(projectId, requesterId);

        assertThatThrownBy(() -> projectService.getProject(projectId, requesterId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getProject_notFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(projectId, requesterId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}