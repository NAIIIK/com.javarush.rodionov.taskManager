package com.javarush.taskmanager.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(
                description = "The email used at registration",
                example = "user@example.com"
        )
        @NotBlank @Email String email,

        @Schema(description = "Plain-text password", example = "P@ssw0rd123")
        @NotBlank String password
) {}