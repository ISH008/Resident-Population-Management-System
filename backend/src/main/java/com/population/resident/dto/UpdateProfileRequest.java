package com.population.resident.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 4, max = 32, message = "username长度应在4-32之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$", message = "username仅支持中文/字母/数字/下划线")
    private String username;

    @Size(max = 64, message = "realName长度不能超过64")
    private String realName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式错误")
    private String phone;
}
