package com.javarush.taskmanager.auth;

import com.javarush.taskmanager.auth.dto.AuthResponse;
import com.javarush.taskmanager.auth.dto.LoginRequest;
import com.javarush.taskmanager.auth.dto.RegisterRequest;
import com.javarush.taskmanager.exception.InvalidCredentialsException;
import com.javarush.taskmanager.security.JwtService;
import com.javarush.taskmanager.user.GlobalRole;
import com.javarush.taskmanager.user.User;
import com.javarush.taskmanager.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenTtlDays", 30L);
    }

    @Test
    void register_newEmail_createsUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", "Alice", "Smith");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        verify(userRepository).save(argThat(u -> u.getGlobalRole() == GlobalRole.USER));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_emailAlreadyExists_throwsIllegalStateException() {
        RegisterRequest request = new RegisterRequest("bob@example.com", "password123", "Bob", "Jones");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void login_correctPassword_returnsTokens() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("carl@example.com")
                .password("hashed-password")
                .globalRole(GlobalRole.USER)
                .build();
        LoginRequest request = new LoginRequest("carl@example.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("nobody@example.com", "password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("dave@example.com")
                .password("hashed-password")
                .globalRole(GlobalRole.USER)
                .build();
        LoginRequest request = new LoginRequest("dave@example.com", "wrong-password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}