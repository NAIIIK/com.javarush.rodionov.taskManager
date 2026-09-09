package com.javarush.taskmanager.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID taskId,
        UUID authorId,
        String text,
        Instant createdAt
) {}