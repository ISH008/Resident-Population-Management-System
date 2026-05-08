package com.population.resident.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JudgeRequest {
    private Boolean temporaryVisitorOnSurveyNight;
    private Boolean bornAfterSurveyTime;
    private Boolean diedAfterSurveyTime;
    private Boolean activeMilitary;
    private Boolean hkMoTwResident;
    private Boolean foreignResident;
    private Boolean inCurrentTown;
    private Boolean usuallyLivesHere;
    /**
     * 临时不在户（经常居住但调查时点未在户）
     */
    private Boolean temporarilyAwayFromHousehold;
    /**
     * 临时不在户原因:
     * BUSINESS_TRIP / FAMILY_VISIT / TRAVEL / NIGHT_SHIFT / OTHER
     */
    private String temporaryAwayReason;
    private Boolean hukouInCurrentTown;
    private Boolean hukouPending;
    /**
     * 户口待定依据类型:
     * MIGRATION_CERT(户口迁移证) / BIRTH_CERT(出生证) / DISCHARGE_CERT(退伍证) / OTHER
     */
    private String hukouPendingProofType;
    private Boolean leftHukouTownOverHalfYear;
    private Boolean outOfHukouTownLessThanHalfYear;
    /**
     * 离开户籍地日期
     */
    private LocalDate leftHukouTownDate;
    /**
     * 迁移详情（附加项）
     */
    private String migrationFromRegion;
    private String migrationToRegion;
    /**
     * 判定提交时是否同步写入迁移记录
     */
    private Boolean syncToMobilityLog;
    private Boolean overseasStudyOrWork;
    private Boolean fullHouseholdAwayOverHalfYear;
    private Boolean fullHouseholdDeceased;
    private Boolean unableToDetermineResidence;
    private Boolean studentBoarding;
    private Boolean returnedHukouTownAndLivedOverHalfYear;
    private Boolean occasionalReturnOnly;
    private Boolean movedAfterSurveyTime;
    private Boolean rentalHouseLandlordHukouAtThisAddress;
}
