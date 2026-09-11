package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskPriority;
import com.javarush.taskmanager.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        @Schema(description = "Task id") UUID id,
        @Schema(description = "Id of the project this task belongs to") UUID projectId,
        @Schema(description = "Task title") String title,
        @Schema(description = "Task description (may be null)") String description,
        @Schema(description = "Current task status") TaskStatus status,
        @Schema(description = "Task priority") TaskPriority priority,
        @Schema(description = "Id of the ProjectMember record of the current assignee (not a User id); null if unassigned")
        UUID assigneeId,
        @Schema(description = "Due date (may be null)") LocalDate dueDate,
        @Schema(description = "Timestamp when the task was created") Instant createdAt
) {}