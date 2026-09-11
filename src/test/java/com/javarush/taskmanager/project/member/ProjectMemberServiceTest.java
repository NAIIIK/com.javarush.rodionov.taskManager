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

    @Test
    void addMember_validRequest_addsMemberAndReturnsResponse() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Project").build();
        User user = User.builder().id(UUID.randomUUID()).email("new@example.com").build();
        AddMemberRequest request = new AddMemberRequest("new@example.com", ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(false);
        when(projectMemberMapper.toResponse(any(ProjectMember.class)))
                .thenReturn(new MemberResponse(
                        UUID.randomUUID(), user.getId(), "new@example.com",
                        "First", "Last", ProjectRole.MEMBER, Instant.now()));

        MemberResponse response = projectMemberService.addMember(projectId, requesterId, request);

        assertThat(response.role()).isEqualTo(ProjectRole.MEMBER);
        verify(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);
        verify(projectMemberRepository).save(argThat(m ->
                m.getProject().equals(project) && m.getUser().equals(user) && m.getRole() == ProjectRole.MEMBER));
    }

    @Test
    void addMember_projectNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        AddMemberRequest request = new AddMemberRequest("new@example.com", ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectAccessGuard, never()).requireRoleAtLeast(any(), any(), any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_requesterLacksRole_throwsAccessDenied() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Project").build();
        AddMemberRequest request = new AddMemberRequest("new@example.com", ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException("Requires role MANAGER or higher"))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_requestedRoleOwner_throwsIllegalArgument() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Project").build();
        AddMemberRequest request = new AddMemberRequest("new@example.com", ProjectRole.OWNER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_userNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Project").build();
        AddMemberRequest request = new AddMemberRequest("missing@example.com", ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_userAlreadyMember_throwsIllegalState() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).name("Project").build();
        User user = User.builder().id(UUID.randomUUID()).email("existing@example.com").build();
        AddMemberRequest request = new AddMemberRequest("existing@example.com", ProjectRole.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.addMember(projectId, requesterId, request))
                .isInstanceOf(IllegalStateException.class);

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void getAllMembers_memberRequesting_returnsMembers() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder().id(UUID.randomUUID()).role(ProjectRole.MEMBER).build();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findAllByProjectId(projectId)).thenReturn(List.of(member));
        when(projectMemberMapper.toResponse(member))
                .thenReturn(new MemberResponse(
                        member.getId(), UUID.randomUUID(), "e@example.com",
                        "First", "Last", ProjectRole.MEMBER, Instant.now()));

        List<MemberResponse> result = projectMemberService.getAllMembers(projectId, requesterId);

        assertThat(result).hasSize(1);
        verify(projectAccessGuard).requireMembership(projectId, requesterId);
    }

    @Test
    void getAllMembers_projectNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.getAllMembers(projectId, requesterId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectAccessGuard, never()).requireMembership(any(), any());
    }

    @Test
    void getAllMembers_notAMember_throwsAccessDenied() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        doThrow(new AccessDeniedException("You are not a member of this project"))
                .when(projectAccessGuard).requireMembership(projectId, requesterId);

        assertThatThrownBy(() -> projectMemberService.getAllMembers(projectId, requesterId))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository, never()).findAllByProjectId(any());
    }


    @Test
    void updateMemberRole_ownerRequesting_updatesRole() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder().id(memberId).role(ProjectRole.MEMBER).build();
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(member));
        when(projectMemberMapper.toResponse(member))
                .thenReturn(new MemberResponse(
                        memberId, UUID.randomUUID(), "e@example.com",
                        "First", "Last", ProjectRole.MANAGER, Instant.now()));

        MemberResponse response = projectMemberService.updateMemberRole(projectId, requesterId, memberId, request);

        assertThat(response.role()).isEqualTo(ProjectRole.MANAGER);
        assertThat(member.getRole()).isEqualTo(ProjectRole.MANAGER);
        verify(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.OWNER);
    }

    @Test
    void updateMemberRole_projectNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectAccessGuard, never()).requireRoleAtLeast(any(), any(), any());
    }

    @Test
    void updateMemberRole_requesterNotOwner_throwsAccessDenied() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
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
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.OWNER);

        when(projectRepository.existsById(projectId)).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(projectMemberRepository, never()).findByIdAndProjectId(any(), any());
    }

    @Test
    void updateMemberRole_memberNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMemberRole_targetIsOwner_throwsIllegalArgument() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMember owner = ProjectMember.builder().id(memberId).role(ProjectRole.OWNER).build();
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(ProjectRole.MANAGER);

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectMemberService.updateMemberRole(projectId, requesterId, memberId, request))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(owner.getRole()).isEqualTo(ProjectRole.OWNER);
    }

    @Test
    void removeMember_managerRequesting_removesMember() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMember member = ProjectMember.builder().id(memberId).role(ProjectRole.MEMBER).build();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(member));

        projectMemberService.removeMember(projectId, requesterId, memberId);

        verify(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);
        verify(projectMemberRepository).delete(member);
    }

    @Test
    void removeMember_projectNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_requesterLacksRole_throwsAccessDenied() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        doThrow(new AccessDeniedException("Requires role MANAGER or higher"))
                .when(projectAccessGuard).requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_memberNotFound_throwsResourceNotFound() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_targetIsOwner_throwsIllegalArgument() {
        UUID projectId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMember owner = ProjectMember.builder().id(memberId).role(ProjectRole.OWNER).build();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(projectMemberRepository.findByIdAndProjectId(memberId, projectId)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, requesterId, memberId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(projectMemberRepository, never()).delete(any());
    }
}