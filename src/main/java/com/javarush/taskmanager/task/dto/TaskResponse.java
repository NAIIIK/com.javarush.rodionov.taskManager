package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskPriority;
import com.javarush.taskmanager.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId,
        LocalDate dueDate,
        Instant createdAt
) {}