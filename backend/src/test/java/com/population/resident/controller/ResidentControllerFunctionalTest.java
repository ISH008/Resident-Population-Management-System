package com.population.resident.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.population.resident.domain.Resident;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.ResidentUpsertRequest;
import com.population.resident.exception.GlobalExceptionHandler;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import com.population.resident.service.ResidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResidentControllerFunctionalTest {

    private MockMvc mockMvc;
    private ResidentService residentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        residentService = Mockito.mock(ResidentService.class);
        ResidentController controller = new ResidentController(residentService);
        HandlerInterceptor roleInjector = new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String role = request.getHeader("X-ROLE");
                if (role != null) {
                    UserContext.set(CurrentUser.builder()
                            .userId(1L)
                            .username("tester")
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
    void pageQuery_shouldFailForUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/residents")
                        .header("X-ROLE", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void pageQuery_shouldSucceedForAdminRole() throws Exception {
        Resident resident = new Resident();
        resident.setId(1L);
        resident.setName("张三");
        Mockito.when(residentService.pageQuery(any(), any(), any(), any(), any()))
                .thenReturn(PageResponse.<Resident>builder()
                        .records(List.of(resident))
                        .total(1L)
                        .pageNum(1)
                        .pageSize(10)
                        .build());

        mockMvc.perform(get("/api/v1/residents")
                        .header("X-ROLE", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].name").value("张三"));
    }

    @Test
    void create_shouldFailWhenPayloadInvalid() throws Exception {
        ResidentUpsertRequest req = new ResidentUpsertRequest();
        req.setName("");
        req.setIdCard("invalid");
        req.setGender("X");

        mockMvc.perform(post("/api/v1/residents")
                        .header("X-ROLE", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }
}
