package com.javarush.taskmanager.project.member;

import com.javarush.taskmanager.exception.ResourceNotFoundException;
import com.javarush.taskmanager.project.Project;
import com.javarush.taskmanager.project.ProjectRepository;
import com.javarush.taskmanager.project.member.dto.AddMemberRequest;
import com.javarush.taskmanager.project.member.dto.MemberResponse;
import com.javarush.taskmanager.project.member.dto.UpdateMemberRoleRequest;
import com.javarush.taskmanager.user.User;
import com.javarush.taskmanager.user.UserRepository;
import java.util.List;
import java.util.UUID;

import com.javarush.taskmanager.util.ExceptionMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ProjectMemberMapper projectMemberMapper;

    public MemberResponse addMember(UUID projectId, UUID requesterId, AddMemberRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionMessages.PROJECT_NOT_FOUND_MSG + projectId));

        projectAccessGuard.requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        if (request.role() == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Cannot grant OWNER role through this endpoint");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("No user with email: " + request.email()));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new IllegalStateException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(request.role())
                .build();
        projectMemberRepository.save(member);

        return projectMemberMapper.toResponse(member);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getAllMembers(UUID projectId, UUID requesterId) {
        projectExists(projectId);
        projectAccessGuard.requireMembership(projectId, requesterId);

        return projectMemberRepository.findAllByProjectId(projectId).stream()
                .map(projectMemberMapper::toResponse)
                .toList();
    }

    public MemberResponse updateMemberRole(UUID projectId, UUID requesterId, UUID memberId, UpdateMemberRoleRequest request) {
        projectExists(projectId);
        projectAccessGuard.requireRoleAtLeast(projectId, requesterId, ProjectRole.OWNER);

        if (request.role() == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Ownership cannot be transferred through this endpoint");
        }

        ProjectMember member = getMemberOrThrow(projectId, memberId);

        if (member.getRole() == ProjectRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the role of the project owner");
        }

        member.setRole(request.role());
        return projectMemberMapper.toResponse(member);
    }

    public void removeMember(UUID projectId, UUID requesterId, UUID memberId) {
        projectExists(projectId);
        projectAccessGuard.requireRoleAtLeast(projectId, requesterId, ProjectRole.MANAGER);

        ProjectMember member = getMemberOrThrow(projectId, memberId);

        if (member.getRole() == ProjectRole.OWNER) {
            throw new IllegalArgumentException("The project owner cannot be removed");
        }

        projectMemberRepository.delete(member);
    }

    private ProjectMember getMemberOrThrow(UUID projectId, UUID memberId) {
        return projectMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in this project: " + memberId));
    }

    private void projectExists(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(ExceptionMessages.PROJECT_NOT_FOUND_MSG + projectId);
        }
    }
}