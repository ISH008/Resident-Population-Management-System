package com.population.resident.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResidentJudgeApplication {
    private Long id;
    private Long residentId;
    private String residentName;
    private String residentIdCard;
    private Long applicantId;
    private String applicantUsername;
    private String applyReason;
    private String evidenceText;
    private Integer localEmploySocial;
    private Integer localActivity90d;
    private String judgeVersion;
    private String status;
    private String reviewComment;
    private Long reviewerId;
    private String reviewerUsername;
    private Integer attachmentCount;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
