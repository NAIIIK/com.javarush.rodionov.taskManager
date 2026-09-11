package com.javarush.taskmanager.integration;

import com.javarush.taskmanager.auth.dto.AuthResponse;
import com.javarush.taskmanager.auth.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public abstract class AbstractAuthenticatedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected JsonMapper jsonMapper;

    protected String registerAndLogin(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(email, DEFAULT_PASSWORD, "First", "Last");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andReturn();

        AuthResponse response = jsonMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return response.accessToken();
    }
}