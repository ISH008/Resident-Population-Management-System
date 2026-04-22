package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JudgeApplicationCreateRequest {
    private Long residentId;

    @NotBlank(message = "applyReason不能为空")
    @Size(max = 500, message = "applyReason长度不能超过500")
    private String applyReason;

    @Size(max = 1000, message = "evidenceText长度不能超过1000")
    private String evidenceText;

    private Boolean localEmploySocial;
    private Boolean localActivity90d;

    @Size(max = 32, message = "judgeVersion长度不能超过32")
    private String judgeVersion;
}
