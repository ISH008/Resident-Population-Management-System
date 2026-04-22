package com.population.resident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.population.resident.dto.ChangePasswordRequest;
import com.population.resident.dto.LoginRequest;
import com.population.resident.dto.LoginResponse;
import com.population.resident.dto.RegisterRequest;
import com.population.resident.dto.UpdateProfileRequest;
import com.population.resident.exception.GlobalExceptionHandler;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import com.population.resident.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerFunctionalTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clear() {
        UserContext.clear();
    }

    @Test
    void login_shouldReturnStandardSuccessPayload() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("t1")
                .expiresIn(7200L)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(1L)
                        .username("u1")
                        .roles(List.of("USER"))
                        .currentRole("USER")
                        .build())
                .build();
        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest() {{
                            setUsername("u1");
                            setPassword("123456");
                        }})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("t1"));
    }

    @Test
    void register_shouldReturnCreatedUserId() throws Exception {
        Mockito.when(authService.register(any(RegisterRequest.class))).thenReturn(99L);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("中文_user");
        req.setPassword("123456");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(99L));
    }

    @Test
    void changePassword_shouldFailWhenNoRoleContext() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setNewPassword("newPass1");

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void updateProfile_shouldSucceedForUserRole() throws Exception {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u")
                .roles(List.of("USER"))
                .currentRole("USER")
                .build());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("新用户名");
        req.setRealName("张三");
        req.setPhone("13800000000");

        mockMvc.perform(post("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}

