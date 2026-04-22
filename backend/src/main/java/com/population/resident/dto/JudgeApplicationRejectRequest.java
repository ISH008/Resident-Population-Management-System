package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JudgeApplicationRejectRequest {
    @NotBlank(message = "reviewComment不能为空")
    @Size(max = 500, message = "reviewComment长度不能超过500")
    private String reviewComment;
}
