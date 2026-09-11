package com.javarush.taskmanager.auth;

import com.javarush.taskmanager.auth.dto.AuthResponse;
import com.javarush.taskmanager.auth.dto.LoginRequest;
import com.javarush.taskmanager.auth.dto.RefreshRequest;
import com.javarush.taskmanager.auth.dto.RegisterRequest;
import com.javarush.taskmanager.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Auth",
        description = """
                Registration, login and JWT token management.
                All endpoints in this group are public and do not require a Bearer token.
                """
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = """
                    Creates a new user with the USER role
                    and immediately issues a token pair (access + refresh).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User created, tokens issued",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email is already taken by another user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Log in with email and password",
            description = "Verifies the credentials and issues a new access/refresh token pair."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, tokens issued",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Refresh the access token using a refresh token",
            description = """
                    Accepts a refresh token, verifies it exists, is not revoked and has not expired,
                    revokes it (single use) and issues a new token pair.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = """
                            Refresh token is blank, not found in the database,
                            already revoked (including reuse of a used token), or expired
                            """,
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(
            summary = "Log out (revoke a refresh token)",
            description = "Revokes the given refresh token so it can no longer be used to refresh the access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token revoked (or was already revoked/did not exist)"),
            @ApiResponse(responseCode = "400", description = "Refresh token is blank",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirements
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}