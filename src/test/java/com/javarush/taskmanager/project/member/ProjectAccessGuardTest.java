package com.javarush.taskmanager.project.member;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjectAccessGuardTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectAccessGuard guard;

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void requireMembership_doesNotThrow_whenUserIsMember() {
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId))
                .thenReturn(true);

        assertThatCode(() -> guard.requireMembership(projectId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    void requireMembership_throwsAccessDenied_whenUserIsNotMember() {
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId))
                .thenReturn(false);

        assertThatThrownBy(() -> guard.requireMembership(projectId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not a member of this project");
    }

    @Test
    void requireRoleAtLeast_doesNotThrow_whenMemberRoleIsHigher() {
        ProjectMember member = mockMemberWithRole(ProjectRole.OWNER);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(member));

        assertThatCode(() -> guard.requireRoleAtLeast(projectId, userId, ProjectRole.MANAGER))
                .doesNotThrowAnyException();
    }

    @Test
    void requireRoleAtLeast_doesNotThrow_whenMemberRoleIsEqual() {
        ProjectMember member = mockMemberWithRole(ProjectRole.MANAGER);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(member));

        assertThatCode(() -> guard.requireRoleAtLeast(projectId, userId, ProjectRole.MANAGER))
                .doesNotThrowAnyException();
    }

    @Test
    void requireRoleAtLeast_throwsAccessDenied_whenMemberRoleIsLower() {
        ProjectMember member = mockMemberWithRole(ProjectRole.MEMBER);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(() -> guard.requireRoleAtLeast(projectId, userId, ProjectRole.MANAGER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Requires role MANAGER or higher");
    }

    @Test
    void requireRoleAtLeast_throwsAccessDenied_whenUserIsNotMember() {
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireRoleAtLeast(projectId, userId, ProjectRole.MANAGER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not a member of this project");
    }

    private ProjectMember mockMemberWithRole(ProjectRole role) {
        ProjectMember member = mock(ProjectMember.class);
        when(member.getRole()).thenReturn(role);
        return member;
    }
}