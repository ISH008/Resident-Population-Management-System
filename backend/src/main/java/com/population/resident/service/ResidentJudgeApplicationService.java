package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.domain.ResidentJudgeApplicationAttachment;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.JudgeApplicationAttachmentItem;
import com.population.resident.dto.JudgeApplicationApproveRequest;
import com.population.resident.dto.JudgeApplicationCreateRequest;
import com.population.resident.dto.JudgeApplicationRejectRequest;
import com.population.resident.dto.JudgeRequest;
import com.population.resident.dto.ManualJudgeRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeApplicationMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.mapper.ResidentJudgeApplicationAttachmentMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResidentJudgeApplicationService {

    private final ResidentJudgeApplicationMapper residentJudgeApplicationMapper;
    private final ResidentJudgeApplicationAttachmentMapper residentJudgeApplicationAttachmentMapper;
    private final ResidentMapper residentMapper;
    private final SysUserMapper sysUserMapper;
    private final ResidentService residentService;
    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    @Transactional(rollbackFor = Exception.class)
    public Long create(JudgeApplicationCreateRequest request) {
        CurrentUser currentUser = requireCurrentUser();
        Long targetResidentId = request.getResidentId();
        if ("USER".equalsIgnoreCase(currentUser.getCurrentRole())) {
            SysUser currentDbUser = sysUserMapper.findById(currentUser.getUserId());
            if (currentDbUser == null || currentDbUser.getResidentId() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "当前账号未绑定居民档案，请联系管理员");
            }
            if (targetResidentId != null && !targetResidentId.equals(currentDbUser.getResidentId())) {
                throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "仅可提交本人判定申请");
            }
            targetResidentId = currentDbUser.getResidentId();
        }
        if (targetResidentId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "residentId不能为空");
        }
        Resident resident = residentMapper.findById(targetResidentId);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "人口档案不存在");
        }

        ResidentJudgeApplication application = new ResidentJudgeApplication();
        application.setResidentId(targetResidentId);
        application.setApplicantId(currentUser.getUserId());
        application.setApplyReason(request.getApplyReason());
        application.setEvidenceText(request.getEvidenceText());
        application.setLocalEmploySocial(boolToInt(request.getLocalEmploySocial()));
        application.setLocalActivity90d(boolToInt(request.getLocalActivity90d()));
        application.setJudgeVersion(normalizeVersion(request.getJudgeVersion()));
        application.setStatus("PENDING");
        residentJudgeApplicationMapper.insert(application);
        return application.getId();
    }

    public PageResponse<ResidentJudgeApplication> pageMine(Integer pageNum, Integer pageSize, String status) {
        int validPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (validPageNum - 1) * validPageSize;
        CurrentUser currentUser = requireCurrentUser();
        Long applicantId = currentUser.getUserId();
        Long residentId = null;
        if ("USER".equalsIgnoreCase(currentUser.getCurrentRole())) {
            SysUser currentDbUser = sysUserMapper.findById(currentUser.getUserId());
            if (currentDbUser == null || currentDbUser.getResidentId() == null) {
                return PageResponse.<ResidentJudgeApplication>builder()
                        .total(0L)
                        .pageNum(validPageNum)
                        .pageSize(validPageSize)
                        .records(java.util.Collections.emptyList())
                        .build();
            }
            residentId = currentDbUser.getResidentId();
        }

        List<ResidentJudgeApplication> records = residentJudgeApplicationMapper.pageMine(
                offset,
                validPageSize,
                applicantId,
                status,
                residentId
        );
        Long total = residentJudgeApplicationMapper.countMine(applicantId, status, residentId);
        return PageResponse.<ResidentJudgeApplication>builder()
                .total(total)
                .pageNum(validPageNum)
                .pageSize(validPageSize)
                .records(records)
                .build();
    }

    public PageResponse<ResidentJudgeApplication> pageAdmin(Integer pageNum,
                                                            Integer pageSize,
                                                            String status,
                                                            String residentName,
                                                            String applicantUsername) {
        int validPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (validPageNum - 1) * validPageSize;

        List<ResidentJudgeApplication> records = residentJudgeApplicationMapper.pageAdmin(
                offset,
                validPageSize,
                status,
                residentName,
                applicantUsername
        );
        Long total = residentJudgeApplicationMapper.countAdmin(status, residentName, applicantUsername);
        return PageResponse.<ResidentJudgeApplication>builder()
                .total(total)
                .pageNum(validPageNum)
                .pageSize(validPageSize)
                .records(records)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, JudgeApplicationApproveRequest request) {
        ResidentJudgeApplication application = residentJudgeApplicationMapper.findById(id);
        if (application == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "申请已处理");
        }
        CurrentUser reviewer = requireCurrentUser();
        String approveMode = request.getApproveMode().toUpperCase(Locale.ROOT);
        String reviewComment = request.getReviewComment();

        if ("AUTO".equals(approveMode)) {
            JudgeRequest judgeRequest = new JudgeRequest();
            judgeRequest.setLocalEmploySocial(resolveBool(request.getLocalEmploySocial(), application.getLocalEmploySocial()));
            judgeRequest.setLocalActivity90d(resolveBool(request.getLocalActivity90d(), application.getLocalActivity90d()));
            judgeRequest.setVersion(resolveVersion(request.getJudgeVersion(), application.getJudgeVersion()));
            residentService.judge(application.getResidentId(), judgeRequest);
            if (!StringUtils.hasText(reviewComment)) {
                reviewComment = "审核通过（自动判定）";
            }
        } else if ("MANUAL".equals(approveMode)) {
            if (!StringUtils.hasText(request.getManualStatus())) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "manualStatus不能为空");
            }
            String manualReason = StringUtils.hasText(request.getManualReason())
                    ? request.getManualReason()
                    : "申请审核通过，管理员人工覆核";
            ManualJudgeRequest manualJudgeRequest = new ManualJudgeRequest();
            manualJudgeRequest.setResidenceStatus(request.getManualStatus());
            manualJudgeRequest.setJudgeReason(manualReason);
            residentService.manualJudge(application.getResidentId(), manualJudgeRequest);
            if (!StringUtils.hasText(reviewComment)) {
                reviewComment = "审核通过（人工覆核）";
            }
        } else {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "approveMode取值错误");
        }

        int updated = residentJudgeApplicationMapper.updateReviewed(
                id,
                "APPROVED",
                reviewComment,
                reviewer.getUserId()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "申请已被处理");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, JudgeApplicationRejectRequest request) {
        ResidentJudgeApplication application = residentJudgeApplicationMapper.findById(id);
        if (application == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "申请已处理");
        }
        CurrentUser reviewer = requireCurrentUser();
        int updated = residentJudgeApplicationMapper.updateReviewed(
                id,
                "REJECTED",
                request.getReviewComment(),
                reviewer.getUserId()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "申请已被处理");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long uploadAttachment(Long applicationId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "附件不能为空");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "附件大小不能超过20MB");
        }
        ResidentJudgeApplication application = requireApplicationAndPermission(applicationId, true);
        CurrentUser currentUser = requireCurrentUser();

        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "unnamed";
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < originalName.length() - 1) {
            ext = "." + originalName.substring(dotIdx + 1);
        }
        String safeFileName = UUID.randomUUID() + ext;
        String dateDir = java.time.LocalDate.now().toString().replace("-", "");
        Path baseDir = Paths.get(System.getProperty("user.dir"), "uploads", "judge-applications", dateDir);
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(safeFileName).toAbsolutePath().normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ResidentJudgeApplicationAttachment attachment = new ResidentJudgeApplicationAttachment();
            attachment.setApplicationId(application.getId());
            attachment.setOriginalName(originalName);
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(target.toString());
            attachment.setUploaderId(currentUser.getUserId());
            residentJudgeApplicationAttachmentMapper.insert(attachment);
            return attachment.getId();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(), "附件保存失败");
        }
    }

    public List<JudgeApplicationAttachmentItem> listAttachments(Long applicationId) {
        ResidentJudgeApplication application = requireApplicationAndPermission(applicationId, false);
        return residentJudgeApplicationAttachmentMapper.findByApplicationId(application.getId()).stream()
                .map(item -> JudgeApplicationAttachmentItem.builder()
                        .id(item.getId())
                        .originalName(item.getOriginalName())
                        .contentType(item.getContentType())
                        .fileSize(item.getFileSize())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();
    }

    public File downloadAttachment(Long attachmentId) {
        ResidentJudgeApplicationAttachment attachment = residentJudgeApplicationAttachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        requireApplicationAndPermission(attachment.getApplicationId(), false);
        File file = new File(attachment.getStoragePath());
        if (!file.exists() || !file.isFile()) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "附件文件不存在");
        }
        return file;
    }

    public ResidentJudgeApplicationAttachment attachmentDetail(Long attachmentId) {
        ResidentJudgeApplicationAttachment attachment = residentJudgeApplicationAttachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        requireApplicationAndPermission(attachment.getApplicationId(), false);
        return attachment;
    }

    private Integer boolToInt(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private Boolean resolveBool(Boolean requestValue, Integer applicationValue) {
        if (requestValue != null) {
            return requestValue;
        }
        return applicationValue != null ? applicationValue == 1 : Boolean.FALSE;
    }

    private String normalizeVersion(String version) {
        return StringUtils.hasText(version) ? version : "v1";
    }

    private String resolveVersion(String requestVersion, String applicationVersion) {
        if (StringUtils.hasText(requestVersion)) {
            return requestVersion;
        }
        return normalizeVersion(applicationVersion);
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    private ResidentJudgeApplication requireApplicationAndPermission(Long applicationId, boolean requirePending) {
        ResidentJudgeApplication application = residentJudgeApplicationMapper.findById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        CurrentUser currentUser = requireCurrentUser();
        if ("USER".equalsIgnoreCase(currentUser.getCurrentRole())
                && !currentUser.getUserId().equals(application.getApplicantId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "无权访问该申请");
        }
        if (requirePending && !"PENDING".equals(application.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "仅待处理申请可上传附件");
        }
        return application;
    }
}
