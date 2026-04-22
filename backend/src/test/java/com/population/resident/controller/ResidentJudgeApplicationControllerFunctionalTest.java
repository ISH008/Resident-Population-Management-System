package com.population.resident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.dto.JudgeApplicationRejectRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.exception.GlobalExceptionHandler;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import com.population.resident.service.ResidentJudgeApplicationAttachmentService;
import com.population.resident.service.ResidentJudgeApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResidentJudgeApplicationControllerFunctionalTest {

    private MockMvc mockMvc;
    private ResidentJudgeApplicationService service;
    private ResidentJudgeApplicationAttachmentService attachmentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = Mockito.mock(ResidentJudgeApplicationService.class);
        attachmentService = Mockito.mock(ResidentJudgeApplicationAttachmentService.class);
        ResidentJudgeApplicationController controller = new ResidentJudgeApplicationController(service, attachmentService);
        HandlerInterceptor roleInjector = new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String role = request.getHeader("X-ROLE");
                String userId = request.getHeader("X-UID");
                if (role != null) {
                    UserContext.set(CurrentUser.builder()
                            .userId(userId == null ? 1L : Long.parseLong(userId))
                            .username("test")
                            .roles(List.of(role))
                            .currentRole(role)
                            .build());
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                UserContext.clear();
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(roleInjector)
                .build();
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void pageMine_shouldBeAccessibleForUserRole() throws Exception {
        ResidentJudgeApplication app = new ResidentJudgeApplication();
        app.setId(1L);
        app.setApplicantId(10L);
        app.setStatus("PENDING");
        Mockito.when(service.pageMine(anyInt(), anyInt(), nullable(String.class)))
                .thenReturn(PageResponse.<ResidentJudgeApplication>builder()
                        .records(List.of(app))
                        .total(1L)
                        .pageNum(1)
                        .pageSize(10)
                        .build());

        mockMvc.perform(get("/api/v1/judge-applications/mine")
                        .header("X-ROLE", "USER")
                        .header("X-UID", "10")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].id").value(1L));
    }

    @Test
    void pageAdmin_shouldFailForUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/judge-applications")
                        .header("X-ROLE", "USER")
                        .header("X-UID", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void reject_shouldSucceedForAdminRole() throws Exception {
        Mockito.doNothing().when(service).reject(anyLong(), any());
        JudgeApplicationRejectRequest req = new JudgeApplicationRejectRequest();
        req.setReviewComment("资料不足");

        mockMvc.perform(put("/api/v1/judge-applications/3/reject")
                        .header("X-ROLE", "ADMIN")
                        .header("X-UID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Mockito.verify(service).reject(eq(3L), any(JudgeApplicationRejectRequest.class));
    }
}
