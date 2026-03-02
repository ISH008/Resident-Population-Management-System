package com.population.resident.controller;

import com.population.resident.common.ApiResponse;
import com.population.resident.domain.SysOperationLog;
import com.population.resident.dto.PageResponse;
import com.population.resident.security.Authz;
import com.population.resident.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作审计")
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "操作审计分页查询")
    @GetMapping
    public ApiResponse<PageResponse<SysOperationLog>> pageQuery(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String result
    ) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(auditLogService.pageQuery(pageNum, pageSize, module, result));
    }
}
