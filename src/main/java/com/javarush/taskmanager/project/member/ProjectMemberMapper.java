package com.javarush.taskmanager.project.member;

import com.javarush.taskmanager.project.member.dto.MemberResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberMapper {

    public MemberResponse toResponse(ProjectMember member) {
        return new MemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getFirstName(),
                member.getUser().getLastName(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}