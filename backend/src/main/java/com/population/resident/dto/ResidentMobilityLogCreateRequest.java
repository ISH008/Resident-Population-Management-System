package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ResidentMobilityLogCreateRequest {

    @NotBlank(message = "changeType不能为空")
    @Pattern(regexp = "^(INFLOW|OUTFLOW)$", message = "changeType仅支持INFLOW/OUTFLOW")
    private String changeType;

    @NotNull(message = "changeDate不能为空")
    private LocalDate changeDate;

    @NotBlank(message = "fromRegion不能为空")
    @Size(max = 120, message = "fromRegion长度不能超过120")
    private String fromRegion;

    @NotBlank(message = "toRegion不能为空")
    @Size(max = 120, message = "toRegion长度不能超过120")
    private String toRegion;

    @NotBlank(message = "reason不能为空")
    @Size(max = 200, message = "reason长度不能超过200")
    private String reason;

    @Size(max = 500, message = "remark长度不能超过500")
    private String remark;
}

