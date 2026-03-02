package com.population.resident.dto;

import lombok.Data;

@Data
public class JudgeRequest {
    private Boolean localEmploySocial;
    private Boolean localActivity90d;
    private String version;
}
