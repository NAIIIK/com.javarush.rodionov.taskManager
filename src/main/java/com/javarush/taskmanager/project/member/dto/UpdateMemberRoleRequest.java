package com.javarush.taskmanager.project.member.dto;

import com.javarush.taskmanager.project.member.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @Schema(description = "New role for the member. Cannot be used to grant or revoke OWNER.")
        @NotNull ProjectRole role
) {}