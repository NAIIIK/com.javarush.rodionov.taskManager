package com.javarush.taskmanager.project.member.dto;

import com.javarush.taskmanager.project.member.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @Schema(
                description = "Email of an existing registered user to add to the project",
                example = "user@example.com"
        )
        @NotBlank @Email String email,

        @Schema(description = "Role to assign. Cannot be OWNER - ownership is not transferable this way.")
        @NotNull ProjectRole role
) {}