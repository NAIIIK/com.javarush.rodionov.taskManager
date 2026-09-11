package com.javarush.taskmanager.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(
                description = """
                        JWT access token. Sent in the Authorization:
                        Bearer <accessToken> header for all protected endpoints.
                        """
        )
        String accessToken,

        @Schema(
                description = """
                        Single-use refresh token used to obtain
                        a new token pair via POST /api/auth/refresh
                        """
        )
        String refreshToken
) {}