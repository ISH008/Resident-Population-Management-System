package com.population.resident.domain;

import lombok.Data;

@Data
public class ResidentJudgeRule {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private Integer weight;
    private String thresholdValue;
    private Integer enabled;
    private Integer sortOrder;
    private String version;
    private String description;
}
