package com.javarush.taskmanager.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Schema(description = "New project name, up to 255 characters. Omit to leave unchanged.")
        @Size(max = 255) String name,

        @Schema(description = "New project description, up to 1000 characters. Omit to leave unchanged.")
        @Size(max = 1000) String description
) {}