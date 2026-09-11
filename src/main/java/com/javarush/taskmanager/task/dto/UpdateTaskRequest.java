package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @Schema(description = "New title, up to 255 characters. Omit to leave unchanged.")
        @Size(max = 255) String title,

        @Schema(description = "New description, up to 2000 characters. Omit to leave unchanged.")
        @Size(max = 2000) String description,

        @Schema(description = "New priority. Omit to leave unchanged.")
        TaskPriority priority,

        @Schema(description = """
                New assignee, as a ProjectMember id (not a User id) belonging to this task's project.
                Omit to leave the assignee unchanged.
                """)
        UUID assigneeId,

        @Schema(description = "New due date. Omit to leave unchanged.")
        LocalDate dueDate
) {}