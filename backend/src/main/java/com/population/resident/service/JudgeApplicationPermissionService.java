package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeApplicationMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JudgeApplicationPermissionService {

    private final ResidentJudgeApplicationMapper residentJudgeApplicationMapper;

    public CurrentUser requireCurrentUser() {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    public boolean isUserRole(CurrentUser currentUser) {
        return "USER".equalsIgnoreCase(currentUser.getCurrentRole());
    }

    public ResidentJudgeApplication requireApplicationAndPermission(Long applicationId, boolean requirePending) {
        ResidentJudgeApplication application = residentJudgeApplicationMapper.findById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        CurrentUser currentUser = requireCurrentUser();
        if (isUserRole(currentUser) && !currentUser.getUserId().equals(application.getApplicantId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "无权访问该申请");
        }
        if (requirePending && !"PENDING".equals(application.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "仅待处理申请可上传附件");
        }
        return application;
    }
}

