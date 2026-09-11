package com.javarush.taskmanager.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(
                description = "User's email, used as the login. Must be unique.",
                example = "user@example.com"
        )
        @NotBlank @Email String email,

        @Schema(
                description = "Plain-text password, 8 to 100 characters. Stored only as a BCrypt hash.",
                example = "P@ssw0rd123"
        )
        @NotBlank @Size(min = 8, max = 100) String password,

        @Schema(description = "User's first name", example = "John")
        @NotBlank String firstName,

        @Schema(description = "User's last name", example = "Doe")
        @NotBlank String lastName
) {}