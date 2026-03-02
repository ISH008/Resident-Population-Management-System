package com.population.resident.controller;

import com.population.resident.audit.AuditLog;
import com.population.resident.common.ApiResponse;
import com.population.resident.dto.LoginRequest;
import com.population.resident.dto.LoginResponse;
import com.population.resident.dto.SwitchRoleRequest;
import com.population.resident.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public ApiResponse<LoginResponse.UserInfo> me() {
        return ApiResponse.success(authService.me());
    }

    @Operation(summary = "角色切换")
    @AuditLog(module = "AUTH", action = "角色切换")
    @PostMapping("/switch-role")
    public ApiResponse<LoginResponse> switchRole(@Valid @RequestBody SwitchRoleRequest request) {
        return ApiResponse.success(authService.switchRole(request.getTargetRole()));
    }
}
