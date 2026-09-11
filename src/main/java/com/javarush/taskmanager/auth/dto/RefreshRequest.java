package com.javarush.taskmanager.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @Schema(
                description = """
                        A single-use refresh token (random UUID) issued at
                        registration/login/the previous refresh. Used only once:
                        it becomes invalid right after a successful /refresh call.
                        """,
                example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
        )
        @NotBlank String refreshToken
) {}