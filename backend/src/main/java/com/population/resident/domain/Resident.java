package com.population.resident.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Resident {
    private Long id;
    private String name;
    private String idCard;
    private String gender;
    private LocalDate birthday;
    private String phone;
    private String actualAddress;
    private String addressProvince;
    private String addressCity;
    private String addressDistrict;
    private String addressDetail;
    private String residenceType;
    private String status;
    private String residenceStatus;
    private Integer residenceScore;
    private LocalDate stayStartDate;
    private LocalDate stayEndDate;
    private Integer isLocalHukou;
    private String proofType;
    private LocalDateTime lastJudgeTime;
    private String judgeVersion;
    private String judgeReason;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
}
