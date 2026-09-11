package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @Schema(
                description = "New task status",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull TaskStatus status
) {}