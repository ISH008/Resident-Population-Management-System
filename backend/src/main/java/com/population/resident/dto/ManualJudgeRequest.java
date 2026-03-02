package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ManualJudgeRequest {
    @NotBlank(message = "residenceStatus不能为空")
    @Pattern(regexp = "^(RESIDENT|NON_RESIDENT|PENDING)$", message = "residenceStatus取值错误")
    private String residenceStatus;
    @NotBlank(message = "judgeReason不能为空")
    private String judgeReason;
}
