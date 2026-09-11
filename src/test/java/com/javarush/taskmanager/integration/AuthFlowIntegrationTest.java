package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.auth.dto.LoginRequest;
import com.javarush.taskmanager.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String WRONG_PASSWORD = "wrong-password";
    private static final String USER_PREFIX = "user";

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void registerThenLogin_returnsTokens() throws Exception {
        String email = uniqueEmail(USER_PREFIX);

        register(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        login(email, DEFAULT_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        String email = uniqueEmail(USER_PREFIX);

        register(email)
                .andExpect(status().isOk());

        register(email)
                .andExpect(status().isConflict());
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail(USER_PREFIX);

        register(email)
                .andExpect(status().isOk());

        login(email, WRONG_PASSWORD)
                .andExpect(status().isUnauthorized());
    }

    private ResultActions register(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(email, DEFAULT_PASSWORD, "Test", "User");

        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }

    private ResultActions login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);

        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));
    }
}