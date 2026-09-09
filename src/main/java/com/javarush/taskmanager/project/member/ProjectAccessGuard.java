package com.javarush.taskmanager.project.member;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessGuard {

    private static final String NOT_A_MEMBER_MSG = "You are not a member of this project";

    private final ProjectMemberRepository projectMemberRepository;

    public void requireMembership(UUID projectId, UUID userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new AccessDeniedException(NOT_A_MEMBER_MSG);
        }
    }

    public void requireRoleAtLeast(UUID projectId, UUID userId, ProjectRole minRole) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException(NOT_A_MEMBER_MSG));

        if (member.getRole().getWeight() > minRole.getWeight()) {
            throw new AccessDeniedException("Requires role " + minRole + " or higher");
        }
    }
}