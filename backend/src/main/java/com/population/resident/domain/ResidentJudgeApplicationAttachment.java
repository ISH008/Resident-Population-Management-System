package com.population.resident.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResidentJudgeApplicationAttachment {
    private Long id;
    private Long applicationId;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String storagePath;
    private Long uploaderId;
    private LocalDateTime createdAt;
}
