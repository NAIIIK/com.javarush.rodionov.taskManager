package com.javarush.taskmanager.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @Schema(
                description = "New comment text, up to 2000 characters",
                example = "Done, ready for review"
        )
        @NotBlank @Size(max = 2000) String text
) {}