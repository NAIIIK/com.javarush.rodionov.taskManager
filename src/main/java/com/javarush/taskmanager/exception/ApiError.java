package com.javarush.taskmanager.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Common error format returned by all API endpoints")
public record ApiError(
        @Schema(description = "Time the error occurred") Instant timestamp,
        @Schema(description = "HTTP status code", example = "404") int status,
        @Schema(description = "Status reason phrase", example = "Not Found") String error,
        @Schema(
                description = "Human-readable error message",
                example = "Project not found: 3fa85f64-5717-4562-b3fc-2c963f66afa6"
        ) String message,
        @Schema(
                description = "Path of the request that caused the error",
                example = "/api/projects/3fa85f64-5717-4562-b3fc-2c963f66afa6"
        ) String path
) {}