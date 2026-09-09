package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        TaskPriority priority,
        UUID assigneeId,
        LocalDate dueDate
) {}