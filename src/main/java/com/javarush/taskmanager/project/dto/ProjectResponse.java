package com.javarush.taskmanager.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        @Schema(description = "Project id") UUID id,
        @Schema(description = "Project name") String name,
        @Schema(description = "Project description (may be null)") String description,
        @Schema(description = "Id of the user (User) who created the project") UUID ownerId,
        @Schema(description = "Timestamp when the project was created") Instant createdAt
) {}