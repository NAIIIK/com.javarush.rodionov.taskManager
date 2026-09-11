package com.javarush.taskmanager.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        @Schema(description = "Comment id") UUID id,
        @Schema(description = "Id of the task this comment belongs to") UUID taskId,
        @Schema(description = "Id of the author's ProjectMember record (not a User id)") UUID authorId,
        @Schema(description = "Comment text") String text,
        @Schema(description = "Timestamp when the comment was created") Instant createdAt
) {}