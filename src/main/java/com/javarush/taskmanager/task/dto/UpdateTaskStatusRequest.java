package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status
) {}