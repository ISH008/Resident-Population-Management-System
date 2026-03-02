package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentJudgeLog;
import com.population.resident.domain.ResidentJudgeRule;
import com.population.resident.dto.JudgeRequest;
import com.population.resident.dto.JudgeResultResponse;
import com.population.resident.dto.ManualJudgeRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.ResidentUpsertRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeLogMapper;
import com.population.resident.mapper.ResidentJudgeRuleMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentMapper residentMapper;
    private final ResidentJudgeRuleMapper residentJudgeRuleMapper;
    private final ResidentJudgeLogMapper residentJudgeLogMapper;

    public PageResponse<Resident> pageQuery(Integer pageNum, Integer pageSize, String name, String idCard, String residenceStatus) {
        int validPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (validPageNum - 1) * validPageSize;

        List<Resident> records = residentMapper.pageQuery(offset, validPageSize, name, idCard, residenceStatus);
        Long total = residentMapper.count(name, idCard, residenceStatus);
        return PageResponse.<Resident>builder()
                .total(total)
                .pageNum(validPageNum)
                .pageSize(validPageSize)
                .records(records)
                .build();
    }

    public Resident detail(Long id) {
        Resident resident = residentMapper.findById(id);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return resident;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ResidentUpsertRequest request) {
        Resident resident = new Resident();
        fillResident(resident, request);
        CurrentUser currentUser = requireCurrentUser();
        resident.setCreatedBy(currentUser.getUserId());
        resident.setUpdatedBy(currentUser.getUserId());
        resident.setResidenceStatus("PENDING");
        resident.setResidenceScore(0);
        residentMapper.insert(resident);
        return resident.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ResidentUpsertRequest request) {
        Resident old = residentMapper.findById(id);
        if (old == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        fillResident(old, request);
        old.setId(id);
        old.setUpdatedBy(requireCurrentUser().getUserId());
        int updated = residentMapper.updateById(old);
        if (updated == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        int updated = residentMapper.logicalDelete(id, requireCurrentUser().getUserId());
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public JudgeResultResponse judge(Long id, JudgeRequest request) {
        Resident resident = residentMapper.findById(id);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        String version = StringUtils.hasText(request.getVersion()) ? request.getVersion() : "v1";
        List<ResidentJudgeRule> rules = residentJudgeRuleMapper.findEnabledRules(version);
        if (rules.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "没有可用判定规则");
        }

        int score = 0;
        List<String> hitRules = new java.util.ArrayList<>();
        CurrentUser currentUser = requireCurrentUser();
        for (ResidentJudgeRule rule : rules) {
            boolean hit = evaluateRule(rule, resident, request);
            int delta = hit ? rule.getWeight() : 0;
            score += delta;
            if (hit) {
                hitRules.add(rule.getRuleCode());
            }
            residentJudgeLogMapper.insert(buildLog(
                    resident.getId(),
                    rule,
                    hit,
                    delta,
                    score,
                    calculateStatus(score),
                    version,
                    "auto",
                    currentUser.getUserId()
            ));
        }

        String finalStatus = calculateStatus(score);
        String finalReason = hitRules.isEmpty() ? "未命中规则" : "命中规则: " + String.join(",", hitRules);
        int updated = residentMapper.updateJudgeResult(id, finalStatus, score, version, finalReason, currentUser.getUserId());
        if (updated == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }

        return JudgeResultResponse.builder()
                .residentId(id)
                .finalScore(score)
                .finalStatus(finalStatus)
                .judgeVersion(version)
                .hitRules(hitRules)
                .build();
    }

    public List<ResidentJudgeLog> judgeLogs(Long residentId) {
        Resident resident = residentMapper.findById(residentId);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return residentJudgeLogMapper.findByResidentId(residentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualJudge(Long residentId, ManualJudgeRequest request) {
        Resident resident = residentMapper.findById(residentId);
        if (resident == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        CurrentUser currentUser = requireCurrentUser();
        int updated = residentMapper.updateManualJudge(
                residentId,
                request.getResidenceStatus(),
                request.getJudgeReason(),
                currentUser.getUserId()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        residentJudgeLogMapper.insert(ResidentJudgeLogBuilder.manual(
                residentId,
                request.getResidenceStatus(),
                request.getJudgeReason(),
                currentUser.getUserId()
        ));
    }

    private void fillResident(Resident resident, ResidentUpsertRequest request) {
        if (request.getStayStartDate() != null
                && request.getStayEndDate() != null
                && request.getStayEndDate().isBefore(request.getStayStartDate())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "居住结束日期不能早于开始日期");
        }
        resident.setName(request.getName());
        resident.setIdCard(request.getIdCard().toUpperCase(Locale.ROOT));
        resident.setGender(request.getGender());
        resident.setBirthday(request.getBirthday());
        resident.setPhone(request.getPhone());
        resident.setActualAddress(request.getActualAddress());
        resident.setResidenceType(request.getResidenceType());
        resident.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "NORMAL");
        resident.setStayStartDate(request.getStayStartDate());
        resident.setStayEndDate(request.getStayEndDate());
        resident.setIsLocalHukou(request.getIsLocalHukou() == null ? 0 : request.getIsLocalHukou());
        resident.setProofType(request.getProofType());
    }

    private boolean evaluateRule(ResidentJudgeRule rule, Resident resident, JudgeRequest request) {
        String code = rule.getRuleCode();
        if ("STAY_180_DAYS".equals(code)) {
            return stayDays(resident) >= parseThreshold(rule.getThresholdValue(), 180);
        }
        if ("VALID_PROOF".equals(code)) {
            return StringUtils.hasText(resident.getProofType());
        }
        if ("LOCAL_EMPLOY_SOCIAL".equals(code)) {
            return Boolean.TRUE.equals(request.getLocalEmploySocial());
        }
        if ("LOCAL_ACTIVITY_90D".equals(code)) {
            return Boolean.TRUE.equals(request.getLocalActivity90d());
        }
        return false;
    }

    private int stayDays(Resident resident) {
        if (resident.getStayStartDate() == null) {
            return 0;
        }
        LocalDate end = resident.getStayEndDate() == null ? LocalDate.now() : resident.getStayEndDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(resident.getStayStartDate(), end);
        return (int) Math.max(days, 0);
    }

    private int parseThreshold(String threshold, int fallback) {
        try {
            return Integer.parseInt(threshold);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String calculateStatus(int score) {
        if (score >= 70) {
            return "RESIDENT";
        }
        if (score >= 40) {
            return "PENDING";
        }
        return "NON_RESIDENT";
    }

    private ResidentJudgeLog buildLog(Long residentId,
                                      ResidentJudgeRule rule,
                                      boolean hit,
                                      int delta,
                                      int finalScore,
                                      String finalStatus,
                                      String version,
                                      String reason,
                                      Long operatorId) {
        ResidentJudgeLog log = new ResidentJudgeLog();
        log.setResidentId(residentId);
        log.setRuleId(rule.getId());
        log.setRuleCode(rule.getRuleCode());
        log.setHitFlag(hit ? 1 : 0);
        log.setScoreDelta(delta);
        log.setFinalScore(finalScore);
        log.setFinalStatus(finalStatus);
        log.setJudgeReason(reason);
        log.setJudgeVersion(version);
        log.setOperatorId(operatorId);
        return log;
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    private static final class ResidentJudgeLogBuilder {
        private ResidentJudgeLogBuilder() {
        }

        static ResidentJudgeLog manual(Long residentId, String finalStatus, String reason, Long operatorId) {
            ResidentJudgeLog log = new ResidentJudgeLog();
            log.setResidentId(residentId);
            log.setRuleId(null);
            log.setRuleCode("MANUAL_OVERRIDE");
            log.setHitFlag(1);
            log.setScoreDelta(0);
            log.setFinalScore(0);
            log.setFinalStatus(finalStatus);
            log.setJudgeReason(reason);
            log.setJudgeVersion("manual");
            log.setOperatorId(operatorId);
            return log;
        }
    }
}
