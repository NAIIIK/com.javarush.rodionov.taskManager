package com.javarush.taskmanager.task.dto;

import com.javarush.taskmanager.task.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @Schema(
                description = "Task title, up to 255 characters",
                example = "Set up CI pipeline"
        )
        @NotBlank @Size(max = 255) String title,

        @Schema(description = "Optional task description, up to 2000 characters")
        @Size(max = 2000) String description,

        @Schema(description = "Task priority. If not provided, defaults to MEDIUM.")
        TaskPriority priority,

        @Schema(description = """
                IMPORTANT: this is the id of a ProjectMember record (project membership),
                not a User id. The referenced member must belong to this specific project,
                otherwise a 400 Bad Request is returned. If omitted, the task is created
                without an assignee.
                """
        )
        UUID assigneeId,

        @Schema(description = "Optional due date")
        LocalDate dueDate
) {}