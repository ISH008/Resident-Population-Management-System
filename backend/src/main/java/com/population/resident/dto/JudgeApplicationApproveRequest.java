package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JudgeApplicationApproveRequest {
    @NotBlank(message = "approveMode不能为空")
    @Pattern(regexp = "^MANUAL$", message = "approveMode仅支持MANUAL")
    private String approveMode;

    private Boolean localEmploySocial;
    private Boolean localActivity90d;

    @Size(max = 32, message = "judgeVersion长度不能超过32")
    private String judgeVersion;

    @Pattern(regexp = "^(RESIDENT|NON_RESIDENT|PENDING)$", message = "manualStatus取值错误")
    private String manualStatus;

    @Size(max = 500, message = "manualReason长度不能超过500")
    private String manualReason;

    @Size(max = 500, message = "reviewComment长度不能超过500")
    private String reviewComment;
}
