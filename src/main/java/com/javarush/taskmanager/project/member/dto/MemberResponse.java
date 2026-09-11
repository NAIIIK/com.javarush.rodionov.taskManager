package com.javarush.taskmanager.project.member.dto;

import com.javarush.taskmanager.project.member.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        @Schema(description = "ProjectMember id (used as assigneeId when creating tasks)") UUID id,
        @Schema(description = "Id of the underlying User") UUID userId,
        @Schema(description = "Member's email") String email,
        @Schema(description = "Member's first name") String firstName,
        @Schema(description = "Member's last name") String lastName,
        @Schema(description = "Role within the project") ProjectRole role,
        @Schema(description = "When the user joined the project") Instant joinedAt
) {}