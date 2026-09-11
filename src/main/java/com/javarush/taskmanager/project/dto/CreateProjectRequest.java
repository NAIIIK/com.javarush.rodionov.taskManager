package com.javarush.taskmanager.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @Schema(
                description = "Project name, up to 255 characters",
                example = "Website Redesign"
        )
        @NotBlank @Size(max = 255) String name,

        @Schema(
                description = "Optional project description, up to 1000 characters",
                example = "Redesign of the company's marketing website"
        )
        @Size(max = 1000) String description
) {}