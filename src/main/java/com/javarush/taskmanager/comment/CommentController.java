package com.javarush.taskmanager.comment;

import com.javarush.taskmanager.comment.dto.CommentResponse;
import com.javarush.taskmanager.comment.dto.CreateCommentRequest;
import com.javarush.taskmanager.exception.ApiError;
import com.javarush.taskmanager.security.CurrentUserId;
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
        name = "Comments",
        description = "Comments on tasks. All endpoints require a Bearer token and membership in the project that owns the task."
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "Add a comment to a task",
            description = """
                    Creates a comment on behalf of the current user. The comment's author is stored as the user's
                    ProjectMember record in the task's project, so the user must be a member of that project.
                    """
    )
    @ApiResponse(responseCode = "201", description = "Comment created",
            content = @Content(schema = @Schema(implementation = CommentResponse.class)))
    @ApiResponse(responseCode = "400", description = "Comment text is blank or longer than 2000 characters",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The user is not a member of the project that owns this task",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "A task with this id does not exist",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @CurrentUserId UUID userId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(taskId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List comments on a task",
            description = """
                    Returns all comments on a task in the order returned by
                    the repository. Available to any member of the task's project.
                    """
    )
    @ApiResponse(responseCode = "200", description = "List of comments (may be empty)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class))))
    @ApiResponse(responseCode = "401", description = "Access token is missing or invalid",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "The user is not a member of the project that owns this task",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "A task with this id does not exist",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable UUID taskId,
            @Parameter(hidden = true) @CurrentUserId UUID userId) {
        return ResponseEntity.ok(commentService.getComments(taskId, userId));
    }
}