package com.population.resident.controller;

import com.population.resident.audit.AuditLog;
import com.population.resident.common.ApiResponse;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.domain.ResidentJudgeApplicationAttachment;
import com.population.resident.dto.JudgeApplicationAttachmentItem;
import com.population.resident.dto.JudgeApplicationApproveRequest;
import com.population.resident.dto.JudgeApplicationCreateRequest;
import com.population.resident.dto.JudgeApplicationRejectRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.security.Authz;
import com.population.resident.service.ResidentJudgeApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "判定申请")
@RestController
@RequestMapping("/api/v1/judge-applications")
@RequiredArgsConstructor
public class ResidentJudgeApplicationController {

    private final ResidentJudgeApplicationService residentJudgeApplicationService;

    @Operation(summary = "提交判定申请")
    @AuditLog(module = "RESIDENT_JUDGE_APPLY", action = "提交判定申请")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody JudgeApplicationCreateRequest request) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentJudgeApplicationService.create(request));
    }

    @Operation(summary = "我的判定申请")
    @GetMapping("/mine")
    public ApiResponse<PageResponse<ResidentJudgeApplication>> pageMine(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status
    ) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentJudgeApplicationService.pageMine(pageNum, pageSize, status));
    }

    @Operation(summary = "管理员分页查询判定申请")
    @GetMapping
    public ApiResponse<PageResponse<ResidentJudgeApplication>> pageAdmin(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String residentName,
            @RequestParam(required = false) String applicantUsername
    ) {
        Authz.requireAnyRole("ADMIN");
        return ApiResponse.success(residentJudgeApplicationService.pageAdmin(
                pageNum,
                pageSize,
                status,
                residentName,
                applicantUsername
        ));
    }

    @Operation(summary = "审批通过判定申请")
    @AuditLog(module = "RESIDENT_JUDGE_APPLY", action = "审批通过判定申请")
    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @Valid @RequestBody JudgeApplicationApproveRequest request) {
        Authz.requireAnyRole("ADMIN");
        residentJudgeApplicationService.approve(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "驳回判定申请")
    @AuditLog(module = "RESIDENT_JUDGE_APPLY", action = "驳回判定申请")
    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody JudgeApplicationRejectRequest request) {
        Authz.requireAnyRole("ADMIN");
        residentJudgeApplicationService.reject(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "上传申请附件")
    @AuditLog(module = "RESIDENT_JUDGE_APPLY", action = "上传申请附件")
    @PostMapping("/{id}/attachments")
    public ApiResponse<Long> uploadAttachment(@PathVariable("id") Long id,
                                              @RequestPart("file") MultipartFile file) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentJudgeApplicationService.uploadAttachment(id, file));
    }

    @Operation(summary = "查询申请附件")
    @GetMapping("/{id}/attachments")
    public ApiResponse<List<JudgeApplicationAttachmentItem>> listAttachments(@PathVariable("id") Long id) {
        Authz.requireAnyRole("ADMIN", "USER");
        return ApiResponse.success(residentJudgeApplicationService.listAttachments(id));
    }

    @Operation(summary = "下载申请附件")
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable Long attachmentId) throws IOException {
        Authz.requireAnyRole("ADMIN", "USER");
        ResidentJudgeApplicationAttachment detail = residentJudgeApplicationService.attachmentDetail(attachmentId);
        File file = residentJudgeApplicationService.downloadAttachment(attachmentId);
        String encodedName = URLEncoder.encode(detail.getOriginalName(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(new InputStreamResource(new FileInputStream(file)));
    }
}
