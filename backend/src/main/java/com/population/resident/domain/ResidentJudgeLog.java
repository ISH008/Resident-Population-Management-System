package com.population.resident.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResidentJudgeLog {
    private Long id;
    private Long residentId;
    private Long ruleId;
    private String ruleCode;
    private Integer hitFlag;
    private Integer scoreDelta;
    private Integer finalScore;
    private String finalStatus;
    private String judgeReason;
    private String judgeVersion;
    private LocalDateTime judgeTime;
    private Long operatorId;
}
