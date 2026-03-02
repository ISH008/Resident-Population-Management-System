package com.population.resident.controller;

import com.population.resident.audit.AuditLog;
import com.population.resident.common.ApiResponse;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentJudgeLog;
import com.population.resident.dto.JudgeRequest;
import com.population.resident.dto.JudgeResultResponse;
import com.population.resident.dto.ManualJudgeRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.ResidentUpsertRequest;
import com.population.resident.security.Authz;
import com.population.resident.service.ResidentService;
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

@Tag(name = "常住人口")
@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @Operation(summary = "分页查询人口档案")
    @GetMapping
    public ApiResponse<PageResponse<Resident>> pageQuery(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) String residenceStatus) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentService.pageQuery(pageNum, pageSize, name, idCard, residenceStatus));
    }

    @Operation(summary = "人口详情")
    @GetMapping("/{id}")
    public ApiResponse<Resident> detail(@PathVariable Long id) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentService.detail(id));
    }

    @Operation(summary = "新增人口")
    @AuditLog(module = "RESIDENT", action = "新增人口")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody ResidentUpsertRequest request) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(residentService.create(request));
    }

    @Operation(summary = "修改人口")
    @AuditLog(module = "RESIDENT", action = "修改人口")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody ResidentUpsertRequest request) {
        Authz.requireAnyRole("ADMIN");
        residentService.update(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除人口(逻辑删除)")
    @AuditLog(module = "RESIDENT", action = "删除人口")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Authz.requireAnyRole("ADMIN");
        residentService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "执行常住判定")
    @AuditLog(module = "RESIDENT_JUDGE", action = "执行常住判定")
    @PostMapping("/{id}/judge")
    public ApiResponse<JudgeResultResponse> judge(@PathVariable Long id, @RequestBody(required = false) JudgeRequest request) {
        Authz.requireAnyRole("ADMIN");
        JudgeRequest finalRequest = request == null ? new JudgeRequest() : request;
        return ApiResponse.success(residentService.judge(id, finalRequest));
    }

    @Operation(summary = "判定日志")
    @GetMapping("/{id}/judge-log")
    public ApiResponse<List<ResidentJudgeLog>> judgeLogs(@PathVariable("id") Long residentId) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentService.judgeLogs(residentId));
    }

    @Operation(summary = "人工覆核判定")
    @AuditLog(module = "RESIDENT_JUDGE", action = "人工覆核判定")
    @PutMapping("/{id}/judge/manual")
    public ApiResponse<Void> manualJudge(@PathVariable("id") Long residentId, @Valid @RequestBody ManualJudgeRequest request) {
        Authz.requireAnyRole("ADMIN");
        residentService.manualJudge(residentId, request);
        return ApiResponse.success(null);
    }
}
