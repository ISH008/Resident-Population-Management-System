package com.population.resident.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JudgeApplicationAttachmentItem {
    private Long id;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
}
