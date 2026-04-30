package com.population.resident.dto;

import lombok.Data;

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
    private Boolean hukouInCurrentTown;
    private Boolean hukouPending;
    private Boolean leftHukouTownOverHalfYear;
    private Boolean outOfHukouTownLessThanHalfYear;
    private Boolean overseasStudyOrWork;
    private Boolean fullHouseholdAwayOverHalfYear;
    private Boolean fullHouseholdDeceased;
    private Boolean unableToDetermineResidence;
    private Boolean studentBoarding;
    private Boolean hukouAtHome;
    private Boolean returnedHukouTownAndLivedOverHalfYear;
    private Boolean occasionalReturnOnly;
    private Boolean movedAfterSurveyTime;
    private Boolean rentalHouseLandlordHukouAtThisAddress;
}
