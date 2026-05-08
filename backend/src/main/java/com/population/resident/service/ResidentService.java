package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentJudgeLog;
import com.population.resident.domain.ResidentMobilityLog;
import com.population.resident.dto.JudgeRequest;
import com.population.resident.dto.JudgeResultResponse;
import com.population.resident.dto.ManualJudgeRequest;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.ResidentUpsertRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeLogMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.mapper.ResidentMobilityLogMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentMapper residentMapper;
    private final ResidentJudgeLogMapper residentJudgeLogMapper;
    private final ResidentMobilityLogMapper residentMobilityLogMapper;

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
        return judgeByDocumentRules(resident, request, "v2");
    }

    private JudgeResultResponse judgeByDocumentRules(Resident resident, JudgeRequest request, String version) {
        CurrentUser currentUser = requireCurrentUser();
        List<String> hitRules = new java.util.ArrayList<>();
        String finalStatus;
        int finalScore;

        if (isTrue(request.getTemporaryVisitorOnSurveyNight())
                || isTrue(request.getBornAfterSurveyTime())
                || isTrue(request.getActiveMilitary())
                || isTrue(request.getHkMoTwResident())
                || isTrue(request.getForeignResident())
                || isTrue(request.getFullHouseholdDeceased())
                || isTrue(request.getUnableToDetermineResidence())
                || isTrue(request.getFullHouseholdAwayOverHalfYear())) {
            finalStatus = "NON_RESIDENT";
            finalScore = 0;
            hitRules.add("1");
        } else if (isTrue(request.getDiedAfterSurveyTime())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("2");
        } else if (isTrue(request.getHukouInCurrentTown())
                && (isTrue(request.getUsuallyLivesHere())
                || isTrue(request.getInCurrentTown())
                || (isTrue(request.getTemporarilyAwayFromHousehold()) && hasValidTemporaryAwayReason(request.getTemporaryAwayReason())))) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("3");
        } else if (isTrue(request.getInCurrentTown())
                && isTrue(request.getHukouPending())
                && hasValidHukouPendingProof(request.getHukouPendingProofType())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("4");
        } else if (isTrue(request.getInCurrentTown()) && isTrue(request.getLeftHukouTownOverHalfYear())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("5");
        } else if (isTrue(request.getHukouInCurrentTown()) && isTrue(request.getOutOfHukouTownLessThanHalfYear())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("6");
        } else if (isTrue(request.getHukouInCurrentTown()) && isTrue(request.getOverseasStudyOrWork())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("7");
        } else if (isTrue(request.getStudentBoarding()) && isTrue(request.getHukouInCurrentTown())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("8");
        } else if (isTrue(request.getRentalHouseLandlordHukouAtThisAddress())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("9");
        } else if (isTrue(request.getMovedAfterSurveyTime())) {
            finalStatus = "RESIDENT";
            finalScore = 0;
            hitRules.add("10");
        } else if (isTrue(request.getReturnedHukouTownAndLivedOverHalfYear()) && !isTrue(request.getOccasionalReturnOnly())) {
            finalStatus = "NON_RESIDENT";
            finalScore = 0;
            hitRules.add("11");
        } else {
            finalStatus = "PENDING";
            finalScore = 0;
            hitRules.add("12");
        }

        for (String ruleCode : hitRules) {
            residentJudgeLogMapper.insert(buildLog(
                    resident.getId(),
                    ruleCode,
                    true,
                    0,
                    finalScore,
                    finalStatus,
                    version,
                    "auto",
                    currentUser.getUserId()
            ));
        }
        String finalReason = "文档规则判定: " + String.join(",", hitRules);
        int updated = residentMapper.updateJudgeResult(
                resident.getId(),
                finalStatus,
                finalScore,
                version,
                finalReason,
                currentUser.getUserId()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
        syncMobilityLogIfNeeded(resident, request, currentUser.getUserId());
        return JudgeResultResponse.builder()
                .residentId(resident.getId())
                .finalScore(finalScore)
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
        String province = clean(request.getAddressProvince());
        String city = clean(request.getAddressCity());
        String district = clean(request.getAddressDistrict());
        String detail = clean(request.getAddressDetail());
        resident.setAddressProvince(province);
        resident.setAddressCity(city);
        resident.setAddressDistrict(district);
        resident.setAddressDetail(detail);
        resident.setActualAddress(buildAddress(province, city, district, detail, request.getActualAddress()));
        resident.setResidenceType(request.getResidenceType());
        resident.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "NORMAL");
        resident.setStayStartDate(request.getStayStartDate());
        resident.setStayEndDate(request.getStayEndDate());
        resident.setIsLocalHukou(request.getIsLocalHukou() == null ? 0 : request.getIsLocalHukou());
        resident.setProofType(request.getProofType());
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private boolean hasValidHukouPendingProof(String proofType) {
        if (!StringUtils.hasText(proofType)) {
            return false;
        }
        String normalized = proofType.trim().toUpperCase(Locale.ROOT);
        return "MIGRATION_CERT".equals(normalized)
                || "BIRTH_CERT".equals(normalized)
                || "DISCHARGE_CERT".equals(normalized)
                || "OTHER".equals(normalized);
    }

    private boolean hasValidTemporaryAwayReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return false;
        }
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        return "BUSINESS_TRIP".equals(normalized)
                || "FAMILY_VISIT".equals(normalized)
                || "TRAVEL".equals(normalized)
                || "NIGHT_SHIFT".equals(normalized)
                || "OTHER".equals(normalized);
    }

    private void syncMobilityLogIfNeeded(Resident resident, JudgeRequest request, Long operatorId) {
        boolean hitHalfYearRule = isTrue(request.getLeftHukouTownOverHalfYear()) || isTrue(request.getOutOfHukouTownLessThanHalfYear());
        boolean hasMigrationDetail = request.getLeftHukouTownDate() != null
                && StringUtils.hasText(clean(request.getMigrationFromRegion()))
                && StringUtils.hasText(clean(request.getMigrationToRegion()));
        boolean shouldSync = isTrue(request.getSyncToMobilityLog()) || (hitHalfYearRule && hasMigrationDetail);
        if (!shouldSync) {
            return;
        }
        if (!hitHalfYearRule) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "仅在“离开户籍地超半年”或“外出不足半年”场景支持同步迁移记录");
        }
        if (isTrue(request.getLeftHukouTownOverHalfYear()) && isTrue(request.getOutOfHukouTownLessThanHalfYear())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "同步迁移记录时，不能同时勾选“离开户籍地超半年”和“外出不足半年”");
        }
        if (request.getLeftHukouTownDate() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "同步迁移记录时，迁移时间不能为空");
        }
        String fromRegion = clean(request.getMigrationFromRegion());
        String toRegion = clean(request.getMigrationToRegion());
        if (!StringUtils.hasText(fromRegion) || !StringUtils.hasText(toRegion)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "同步迁移记录时，迁出地和迁入地不能为空");
        }
        ResidentMobilityLog log = new ResidentMobilityLog();
        log.setResidentId(resident.getId());
        log.setChangeType(isTrue(request.getLeftHukouTownOverHalfYear()) ? "INFLOW" : "OUTFLOW");
        log.setChangeDate(request.getLeftHukouTownDate());
        log.setFromRegion(fromRegion);
        log.setToRegion(toRegion);
        log.setReason("判定页同步登记");
        log.setRemark("来源:常住判定");
        log.setOperatorId(operatorId);
        residentMobilityLogMapper.insert(log);
    }

    private ResidentJudgeLog buildLog(Long residentId,
                                      String ruleCode,
                                      boolean hit,
                                      int delta,
                                      int finalScore,
                                      String finalStatus,
                                      String version,
                                      String reason,
                                      Long operatorId) {
        ResidentJudgeLog log = new ResidentJudgeLog();
        log.setResidentId(residentId);
        log.setRuleId(null);
        log.setRuleCode(ruleCode);
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

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildAddress(String province, String city, String district, String detail, String fallback) {
        String joined = String.join("",
                province == null ? "" : province,
                city == null ? "" : city,
                district == null ? "" : district);
        if (StringUtils.hasText(joined) || StringUtils.hasText(detail)) {
            if (StringUtils.hasText(detail)) {
                return joined + detail;
            }
            return joined;
        }
        return clean(fallback);
    }

    private static final class ResidentJudgeLogBuilder {
        private ResidentJudgeLogBuilder() {
        }

        static ResidentJudgeLog manual(Long residentId, String finalStatus, String reason, Long operatorId) {
            ResidentJudgeLog log = new ResidentJudgeLog();
            log.setResidentId(residentId);
            log.setRuleId(null);
            log.setRuleCode("手动");
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
