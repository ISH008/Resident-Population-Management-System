package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.JudgeApplicationApproveRequest;
import com.population.resident.dto.JudgeApplicationCreateRequest;
import com.population.resident.dto.JudgeApplicationRejectRequest;
import com.population.resident.dto.JudgeRequest;
import com.population.resident.dto.ManualJudgeRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeApplicationMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResidentJudgeApplicationService {

    private final ResidentJudgeApplicationMapper residentJudgeApplicationMapper;
    private final ResidentMapper residentMapper;
    private final SysUserMapper sysUserMapper;
    private final ResidentService residentService;
    private final JudgeApplicationPermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    public Long create(JudgeApplicationCreateRequest request) {
        CurrentUser currentUser = permissionService.requireCurrentUser();
        Long targetResidentId = request.getResidentId();
        if (permissionService.isUserRole(currentUser)) {
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
        CurrentUser currentUser = permissionService.requireCurrentUser();
        Long applicantId = currentUser.getUserId();
        Long residentId = null;
        if (permissionService.isUserRole(currentUser)) {
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
        CurrentUser reviewer = permissionService.requireCurrentUser();
        String approveMode = request.getApproveMode().toUpperCase(Locale.ROOT);
        String reviewComment = request.getReviewComment();

        if ("AUTO".equals(approveMode)) {
            JudgeRequest judgeRequest = new JudgeRequest();
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
        CurrentUser reviewer = permissionService.requireCurrentUser();
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

    private Integer boolToInt(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private String normalizeVersion(String version) {
        return StringUtils.hasText(version) ? version : "v2";
    }
}
