package com.javarush.taskmanager.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returnsBadRequest() {
        HttpServletRequest request = mockRequest("/api/projects");

        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("bad input");
        assertThat(body.path()).isEqualTo("/api/projects");
        assertThat(body.timestamp()).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void handleIllegalState_returnsConflict() {
        HttpServletRequest request = mockRequest("/api/projects/1");

        ResponseEntity<ApiError> response =
                handler.handleIllegalState(new IllegalStateException("conflict"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo("Conflict");
        assertThat(body.message()).isEqualTo("conflict");
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        HttpServletRequest request = mockRequest("/api/projects/1/members");

        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("no access"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("no access");
    }

    @Test
    void handleUsernameNotFound_returnsUnauthorizedWithGenericMessage() {
        HttpServletRequest request = mockRequest("/api/auth/login");

        ResponseEntity<ApiError> response =
                handler.handleUsernameNotFound(new UsernameNotFoundException("user xyz not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        // сообщение не должно светить реальную причину (username not found)
        assertThat(body.message()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleResourceNotFound_returnsNotFound() {
        HttpServletRequest request = mockRequest("/api/projects/999");

        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("project not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("project not found");
    }

    @Test
    void handleInvalidCredentials_returnsUnauthorized() {
        HttpServletRequest request = mockRequest("/api/auth/login");

        ResponseEntity<ApiError> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Invalid credentials");
    }

    @Test
    void usernameNotFoundAndInvalidCredentials_produceIdenticalResponseShape() {
        HttpServletRequest request = mockRequest("/api/auth/login");

        ResponseEntity<ApiError> byUsername =
                handler.handleUsernameNotFound(new UsernameNotFoundException("no such user"), request);
        ResponseEntity<ApiError> byCredentials =
                handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        ApiError usernameBody = byUsername.getBody();
        ApiError credentialsBody = byCredentials.getBody();

        assertThat(usernameBody).isNotNull();
        assertThat(credentialsBody).isNotNull();
        assertThat(byUsername.getStatusCode()).isEqualTo(byCredentials.getStatusCode());
        assertThat(usernameBody.message()).isEqualTo(credentialsBody.message());
    }

    @Test
    void handleValidation_returnsFirstFieldErrorMessage() {
        HttpServletRequest request = mockRequest("/api/projects");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("projectDto", "name", "must not be blank");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("must not be blank");
    }

    @Test
    void handleValidation_fallsBackToDefaultMessage_whenFieldErrorMessageIsNull() {
        HttpServletRequest request = mockRequest("/api/projects");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("projectDto", "name", null, false, null, null, null);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Invalid value");
    }

    @Test
    void handleValidation_fallsBackToGenericMessage_whenNoFieldErrors() {
        HttpServletRequest request = mockRequest("/api/projects");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Validation failed");
    }

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}