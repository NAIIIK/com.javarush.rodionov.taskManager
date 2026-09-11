package com.javarush.taskmanager.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.javarush.taskmanager.util.ExceptionMessages;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private static final String API_PROJECTS = "/api/projects";
    private static final String API_PROJECTS_1 = "/api/projects/1";
    private static final String API_LOGIN = "/api/auth/login";

    private static final String BAD_INPUT_MSG = "bad input";
    private static final String CONFLICT_MSG = "conflict";
    private static final String NO_ACCESS_MSG = "no access";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
    }

    @Test
    void handleIllegalArgument_returnsBadRequest() {
        when(request.getRequestURI()).thenReturn(API_PROJECTS);

        ResponseEntity<ApiError> response =
                handler.handleIllegalArgument(new IllegalArgumentException(BAD_INPUT_MSG), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ApiError body = response.getBody();
        assertThat(body)
                .isNotNull()
                .satisfies(b -> {
                    assertThat(b.status()).isEqualTo(400);
                    assertThat(b.error()).isEqualTo("Bad Request");
                    assertThat(b.message()).isEqualTo(BAD_INPUT_MSG);
                    assertThat(b.path()).isEqualTo(API_PROJECTS);
                    assertThat(b.timestamp()).isCloseTo(Instant.now(), within(2, ChronoUnit.SECONDS));
                });
    }

    @Test
    void handleIllegalState_returnsConflict() {
        when(request.getRequestURI()).thenReturn(API_PROJECTS_1);

        ResponseEntity<ApiError> response =
                handler.handleIllegalState(new IllegalStateException(CONFLICT_MSG), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(
                        apiError -> apiError != null ? apiError.error() : null,
                        apiError1 -> apiError1 != null ? apiError1.message() : null
                )
                .containsExactly("Conflict", CONFLICT_MSG);
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        when(request.getRequestURI()).thenReturn(API_PROJECTS_1);

        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException(NO_ACCESS_MSG), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo(NO_ACCESS_MSG);
    }

    @Test
    void handleUsernameNotFound_returnsUnauthorizedWithGenericMessage() {
        when(request.getRequestURI()).thenReturn(API_LOGIN);

        ResponseEntity<ApiError> response =
                handler.handleUsernameNotFound(new UsernameNotFoundException("user xyz not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo(ExceptionMessages.INVALID_CREDENTIALS_MSG);
    }

    @Test
    void handleResourceNotFound_returnsNotFound() {
        String message = "project not found";
        when(request.getRequestURI()).thenReturn(API_PROJECTS_1);

        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException(message), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo(message);
    }

    @Test
    void handleInvalidCredentials_returnsUnauthorized() {
        when(request.getRequestURI()).thenReturn(API_LOGIN);

        ResponseEntity<ApiError> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo(ExceptionMessages.INVALID_CREDENTIALS_MSG);
    }

    @Test
    void usernameNotFoundAndInvalidCredentials_produceIdenticalResponseShape() {
        when(request.getRequestURI()).thenReturn(API_LOGIN);

        ResponseEntity<ApiError> byUsername =
                handler.handleUsernameNotFound(new UsernameNotFoundException("no such user"), request);
        ResponseEntity<ApiError> byCredentials =
                handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        assertThat(byUsername.getStatusCode()).isEqualTo(byCredentials.getStatusCode());
        assertThat(byUsername.getBody())
                .isNotNull()
                .usingRecursiveComparison()
                .ignoringFields("timestamp")
                .isEqualTo(byCredentials.getBody());
    }

    @Test
    void handleValidation_returnsFirstFieldErrorMessage() {
        when(request.getRequestURI()).thenReturn(API_PROJECTS);
        String mustNotBeBlankMsg = "must not be blank";

        MethodArgumentNotValidException ex = createValidationException(mustNotBeBlankMsg);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo(mustNotBeBlankMsg);
    }

    @Test
    void handleValidation_fallsBackToDefaultMessage_whenFieldErrorMessageIsNull() {
        when(request.getRequestURI()).thenReturn(API_PROJECTS);
        MethodArgumentNotValidException ex = createValidationException(null);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo("Invalid value");
    }

    @Test
    void handleValidation_fallsBackToGenericMessage_whenNoFieldErrors() {
        when(request.getRequestURI()).thenReturn(API_PROJECTS);

        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getBody())
                .isNotNull()
                .extracting(apiError -> apiError != null ? apiError.message() : null)
                .isEqualTo("Validation failed");
    }

    private MethodArgumentNotValidException createValidationException(String errorMessage) {
        String projectDtoStr = "projectDto";

        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), projectDtoStr);
        bindingResult.addError(new FieldError(projectDtoStr, "name", errorMessage));
        return new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
    }
}