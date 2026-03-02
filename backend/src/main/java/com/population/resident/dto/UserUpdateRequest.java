package com.population.resident.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    @Size(min = 6, max = 32, message = "password长度应在6-32之间")
    private String password;
    private String realName;
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
    private String phone;
    private Integer status;
    @NotEmpty(message = "roleIds不能为空")
    private List<Long> roleIds;
}
