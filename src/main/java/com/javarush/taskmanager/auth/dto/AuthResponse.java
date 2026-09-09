package com.javarush.taskmanager.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}