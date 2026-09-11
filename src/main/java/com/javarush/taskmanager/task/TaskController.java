package com.javarush.taskmanager.task;

import com.javarush.taskmanager.exception.ApiError;
import com.javarush.taskmanager.security.CurrentUserId;
import com.javarush.taskmanager.task.dto.CreateTaskRequest;
import com.javarush.taskmanager.task.dto.TaskResponse;
import com.javarush.taskmanager.task.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Tasks",
        description = """
                Tasks within a project. All endpoints require
                a Bearer token and membership in the corresponding project.
                """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Create a task in a project",
            description = """
                    Creates a task in a project. Requires the OWNER or MANAGER role in that project -
                    members with the MEMBER role cannot access this endpoint.
                    The assigneeId field (if provided) is the id of a ProjectMember record (NOT a User id),
                    and the assigned member must belong to this specific project.
                    If priority is not provided, MEDIUM is used; a new task's status is always TO_DO.
                    """
    )
    @ApiResponse(responseCode = "201", description = "Task created",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = """
                           Validation error on title/description, OR assigneeId
                           refers to a member who does not belong to this project
                           """,
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The user is not a member of the project, or their role is below MANAGER",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Project does not exist",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID projectId,
            @Parameter(hidden = true) @CurrentUserId UUID userId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(projectId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List tasks of a project",
            description = "Returns all tasks of a project."
    )
    @ApiResponse(responseCode = "200", description = "List of tasks (may be empty)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class))))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The user is not a member of this project",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable UUID projectId,
            @Parameter(hidden = true) @CurrentUserId UUID userId) {
        return ResponseEntity.ok(taskService.getTasksForProject(projectId, userId));
    }

    @Operation(
            summary = "Assign a task to yourself",
            description = """
                    Makes the current user the assignee of the task. Only requires membership
                    in the task's project (role does not matter); the previous assignee is overwritten.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Task assigned to the current user",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The user is not a member of the project that owns this task",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "A task with this id does not exist",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PatchMapping("/api/tasks/{taskId}/assign-self")
    public ResponseEntity<TaskResponse> assignSelf(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @CurrentUserId UUID userId) {
        return ResponseEntity.ok(taskService.assignSelf(taskId, userId));
    }

    @Operation(
            summary = "Update task status",
            description = """
                    Changes the task status (TO_DO, IN_PROGRESS, IN_CODE_REVIEW, DONE).
                    The status can be changed only by the task's assignee or by a project
                    member with the OWNER/MANAGER role;
                    """
    )
    @ApiResponse(responseCode = "200", description = "Status updated",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiResponse(responseCode = "400", description = "The status field is missing or contains an invalid value",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(
            responseCode = "403",
            description = """
                           The user is not a member of the project,
                           or is neither the task's assignee nor OWNER/MANAGER
                           """,
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(responseCode = "404", description = "A task with this id does not exist",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PatchMapping("/api/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @CurrentUserId UUID userId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(taskId, userId, request));
    }
}