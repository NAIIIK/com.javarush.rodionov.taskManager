package com.javarush.taskmanager.project.member;

import com.javarush.taskmanager.exception.ResourceNotFoundException;
import com.javarush.taskmanager.project.Project;
import com.javarush.taskmanager.project.ProjectRepository;
import com.javarush.taskmanager.project.member.dto.AddMemberRequest;
import com.javarush.taskmanager.project.member.dto.MemberResponse;
import com.javarush.taskmanager.project.member.dto.UpdateMemberRoleRequest;
import com.javarush.taskmanager.user.User;
import com.javarush.taskmanager.user.UserRepository;
import java.time.Instant;
import java.util.List;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    private static final String NEW_USER_EMAIL = "new@example.com";
    private static final String MISSING_EMAIL = "missing@example.com";
    private static final String FIRST_NAME = "First";
    private static final String LAST_NAME = "Last";

    private static final String REQUIRES_MANAGER_ROLE_OR_HIGHER_MSG = "Requires role MANAGER or higher";

    private final UUID projectId = UUID.randomUUID();
    private final UUID requesterId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectAccessGuard projectAccessGuard;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        project = Project.builder().id(projectId).name("Project").build();
        user = User.builder().id(userId).email(NEW_USER_EMAIL).build();
    }

    @Test
    void addMember_validRequest_addsMemberAndReturnsResponse() {
        AddMemberRequest request = new AddMemberRequest(NEW_USER_EMAIL, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(NEW_USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(false);
        when(projectMemberMapper.toResponse(any(ProjectMember.class)))
                .thenReturn(createMemberResponse(ProjectRole.MEMBER));

        MemberResponse response = projectMemberService.addMember(projectId, requesterId, request);

        assertThat(response.role()).isEqualTo(ProjectRole.MEMBER);
        verify(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);
        verify(projectMemberRepository).save(argThat(m ->
                m.getProject().equals(project) && m.getUser().equals(user) && m.getRole() == ProjectRole.MEMBER));
    }

    @Test
    void addMember_projectNotFound_throwsResourceNotFound() {
        AddMemberRequest request = new AddMemberRequest(NEW_USER_EMAIL, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectAccessGuard, never()).requireRoleAtLeast(any(), any(), any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_requesterLacksRole_throwsAccessDenied() {
        AddMemberRequest request = new AddMemberRequest(NEW_USER_EMAIL, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException(REQUIRES_MANAGER_ROLE_OR_HIGHER_MSG))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_requestedRoleOwner_throwsIllegalArgument() {
        AddMemberRequest request = new AddMemberRequest(NEW_USER_EMAIL, ProjectRole.OWNER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_userNotFound_throwsResourceNotFound() {
        AddMemberRequest request = new AddMemberRequest(MISSING_EMAIL, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(MISSING_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_userAlreadyMember_throwsIllegalState() {
        AddMemberRequest request = new AddMemberRequest(NEW_USER_EMAIL, ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(NEW_USER_EMAIL)).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(IllegalStateException.class);

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void getAllMembers_memberRequesting_returnsMembers() {
        ProjectMember member = createProjectMember(ProjectRole.MEMBER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findAllByProjectId(projectId)).thenReturn(List.of(member));
        when(projectMemberMapper.toResponse(member)).thenReturn(createMemberResponse(ProjectRole.MEMBER));

        List<MemberResponse> result = projectMemberService.getAllMembers(projectId, requesterId);

        assertThat(result).hasSize(1);
        verify(projectAccessGuard).requireMembership(projectId, requesterId);
    }

    @Test
    void getAllMembers_projectNotFound_throwsResourceNotFound() {
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.getAllMembers(projectId, requesterId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectAccessGuard, never()).requireMembership(any(), any());
    }

    @Test
    void getAllMembers_notAMember_throwsAccessDenied() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        doThrow(new AccessDeniedException(ExceptionMessages.NOT_A_MEMBER_MSG))
                .when(projectAccessGuard).requireMembership(projectId, requesterId);

        assertThatThrownBy(() -> projectMemberService.getAllMembers(projectId, requesterId))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository, never()).findAllByProjectId(any());
    }

    @Test
    void updateMemberRole_ownerRequesting_updatesRole() {
        ProjectMember member = createProjectMember(ProjectRole.MEMBER);
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(member));
        when(projectMemberMapper.toResponse(member)).thenReturn(createMemberResponse(ProjectRole.MANAGER));

        MemberResponse response = projectMemberService.updateMemberRole(projectId, requesterId, memberId, request);

        assertThat(response.role()).isEqualTo(ProjectRole.MANAGER);
        assertThat(member.getRole()).isEqualTo(ProjectRole.MANAGER);
        verify(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.OWNER);
    }

    @Test
    void updateMemberRole_projectNotFound_throwsResourceNotFound() {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectAccessGuard, never()).requireRoleAtLeast(any(), any(), any());
    }

    @Test
    void updateMemberRole_requesterNotOwner_throwsAccessDenied() {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        doThrow(new AccessDeniedException("Requires role OWNER or higher"))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.OWNER);

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository, never()).findByIdAndProjectId(any(), any());
    }

    @Test
    void updateMemberRole_requestedRoleOwner_throwsIllegalArgument() {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.OWNER);

        when(projectRepository.existsById(projectId)).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(projectMemberRepository, never()).findByIdAndProjectId(any(), any());
    }

    @Test
    void updateMemberRole_memberNotFound_throwsResourceNotFound() {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMemberRole_targetIsOwner_throwsIllegalArgument() {
        ProjectMember owner = createProjectMember(ProjectRole.OWNER);
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(owner.getRole()).isEqualTo(ProjectRole.OWNER);
    }

    @Test
    void removeMember_managerRequesting_removesMember() {
        ProjectMember member = createProjectMember(ProjectRole.MEMBER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(member));

        projectMemberService.removeMember(projectId, requesterId, memberId);

        verify(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);
        verify(projectMemberRepository).delete(member);
    }

    @Test
    void removeMember_projectNotFound_throwsResourceNotFound() {
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_requesterLacksRole_throwsAccessDenied() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        doThrow(new AccessDeniedException(REQUIRES_MANAGER_ROLE_OR_HIGHER_MSG))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_memberNotFound_throwsResourceNotFound() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_targetIsOwner_throwsIllegalArgument() {
        ProjectMember owner = createProjectMember(ProjectRole.OWNER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    private ProjectMember createProjectMember(ProjectRole role) {
        return ProjectMember.builder()
                .id(memberId)
                .role(role)
                .build();
    }

    private MemberResponse createMemberResponse(ProjectRole role) {
        return new MemberResponse(
                memberId,
                userId,
                NEW_USER_EMAIL,
                FIRST_NAME,
                LAST_NAME,
                role,
                Instant.now()
        );
    }
}