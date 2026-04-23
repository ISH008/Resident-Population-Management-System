package com.population.resident.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ResidentMobilityLog {
    private Long id;
    private Long residentId;
    private String residentName;
    private String changeType;
    private LocalDate changeDate;
    private String fromRegion;
    private String toRegion;
    private String reason;
    private String remark;
    private Long operatorId;
    private String operatorUsername;
    private LocalDateTime createdAt;
}

