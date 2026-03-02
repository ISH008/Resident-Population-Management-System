package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SwitchRoleRequest {
    @NotBlank(message = "targetRole不能为空")
    private String targetRole;
}
