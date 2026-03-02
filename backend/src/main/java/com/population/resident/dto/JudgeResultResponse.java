package com.population.resident.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JudgeResultResponse {
    private Long residentId;
    private Integer finalScore;
    private String finalStatus;
    private String judgeVersion;
    private List<String> hitRules;
}
