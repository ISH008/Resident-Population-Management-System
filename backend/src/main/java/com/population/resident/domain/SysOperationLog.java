package com.population.resident.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysOperationLog {
    private Long id;
    private Long operatorId;
    private String operatorUsername;
    private String module;
    private String action;
    private String targetId;
    private String requestMethod;
    private String requestPath;
    private String result;
    private String message;
    private LocalDateTime createdAt;
}
