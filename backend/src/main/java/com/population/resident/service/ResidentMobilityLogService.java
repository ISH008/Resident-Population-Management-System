package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentMobilityLog;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.ResidentMobilityLogCreateRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.mapper.ResidentMobilityLogMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResidentMobilityLogService {

    private final ResidentMobilityLogMapper residentMobilityLogMapper;
    private final ResidentMapper residentMapper;
    private final SysUserMapper sysUserMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long residentId, ResidentMobilityLogCreateRequest request) {
        Resident resident = residentMapper.findById(residentId);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        String changeType = request.getChangeType().toUpperCase(Locale.ROOT);
        if (!"INFLOW".equals(changeType) && !"OUTFLOW".equals(changeType)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "changeType取值错误");
        }

        ResidentMobilityLog log = new ResidentMobilityLog();
        log.setResidentId(residentId);
        log.setChangeType(changeType);
        log.setChangeDate(request.getChangeDate());
        log.setFromRegion(clean(request.getFromRegion()));
        log.setToRegion(clean(request.getToRegion()));
        log.setReason(clean(request.getReason()));
        log.setRemark(clean(request.getRemark()));
        log.setOperatorId(requireCurrentUser().getUserId());

        residentMobilityLogMapper.insert(log);
        return log.getId();
    }

    public List<ResidentMobilityLog> listByResident(Long residentId) {
        Resident resident = residentMapper.findById(residentId);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return residentMobilityLogMapper.findByResidentId(residentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long residentId, Long logId) {
        Resident resident = residentMapper.findById(residentId);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        ResidentMobilityLog log = residentMobilityLogMapper.findById(logId);
        if (log == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!residentId.equals(log.getResidentId())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "迁移记录与居民不匹配");
        }
        int deleted = residentMobilityLogMapper.deleteById(logId);
        if (deleted == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public List<ResidentMobilityLog> listMine() {
        CurrentUser currentUser = requireCurrentUser();
        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getResidentId() == null) {
            return Collections.emptyList();
        }
        return residentMobilityLogMapper.findByResidentId(user.getResidentId());
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    private String clean(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return text.trim();
    }
}

