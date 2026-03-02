package com.population.resident.controller;

import com.population.resident.audit.AuditLog;
import com.population.resident.common.ApiResponse;
import com.population.resident.domain.SysRole;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.UserCreateRequest;
import com.population.resident.dto.UserPageItem;
import com.population.resident.dto.UserUpdateRequest;
import com.population.resident.security.Authz;
import com.population.resident.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户角色管理")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页")
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserPageItem>> pageUsers(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(userService.pageQuery(pageNum, pageSize, username, realName, status));
    }

    @Operation(summary = "新增用户")
    @AuditLog(module = "USER", action = "新增用户")
    @PostMapping("/users")
    public ApiResponse<Long> createUser(@Valid @RequestBody UserCreateRequest request) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(userService.create(request));
    }

    @Operation(summary = "编辑用户")
    @AuditLog(module = "USER", action = "编辑用户")
    @PutMapping("/users/{id}")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        Authz.requireAnyRole("ADMIN");
        userService.update(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除用户")
    @AuditLog(module = "USER", action = "删除用户")
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        Authz.requireAnyRole("ADMIN");
        userService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public ApiResponse<List<SysRole>> listRoles() {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(userService.listRoles());
    }
}
