package com.population.resident.controller;

import com.population.resident.audit.AuditLog;
import com.population.resident.common.ApiResponse;
import com.population.resident.domain.ResidentMobilityLog;
import com.population.resident.dto.ResidentMobilityLogCreateRequest;
import com.population.resident.security.Authz;
import com.population.resident.service.ResidentMobilityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "人口迁移记录")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ResidentMobilityLogController {

    private final ResidentMobilityLogService residentMobilityLogService;

    @Operation(summary = "新增迁移记录")
    @AuditLog(module = "RESIDENT_MOBILITY", action = "新增迁移记录")
    @PostMapping("/residents/{residentId}/mobility-logs")
    public ApiResponse<Long> create(@PathVariable Long residentId, @Valid @RequestBody ResidentMobilityLogCreateRequest request) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(residentMobilityLogService.create(residentId, request));
    }

    @Operation(summary = "居民迁移记录列表")
    @GetMapping("/residents/{residentId}/mobility-logs")
    public ApiResponse<List<ResidentMobilityLog>> listByResident(@PathVariable Long residentId) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(residentMobilityLogService.listByResident(residentId));
    }

    @Operation(summary = "删除迁移记录")
    @AuditLog(module = "RESIDENT_MOBILITY", action = "删除迁移记录")
    @DeleteMapping("/residents/{residentId}/mobility-logs/{logId}")
    public ApiResponse<Void> delete(@PathVariable Long residentId, @PathVariable Long logId) {
        Authz.requireAnyRole("ADMIN");
        residentMobilityLogService.delete(residentId, logId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "我的迁移记录")
    @GetMapping("/mobility-logs/me")
    public ApiResponse<List<ResidentMobilityLog>> listMine() {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentMobilityLogService.listMine());
    }
}

