package com.population.resident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ResidentUpsertRequest {
    @NotBlank(message = "name不能为空")
    private String name;
    @NotBlank(message = "idCard不能为空")
    @Pattern(regexp = "^[0-9Xx]{18}$", message = "idCard格式错误")
    private String idCard;
    @NotBlank(message = "gender不能为空")
    @Pattern(regexp = "^(M|F)$", message = "gender仅支持M/F")
    private String gender;
    private LocalDate birthday;
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
    private String phone;
    @Size(max = 32, message = "addressProvince长度不能超过32")
    private String addressProvince;
    @Size(max = 32, message = "addressCity长度不能超过32")
    private String addressCity;
    @Size(max = 32, message = "addressDistrict长度不能超过32")
    private String addressDistrict;
    @Size(max = 255, message = "addressDetail长度不能超过255")
    private String addressDetail;
    private String actualAddress;
    private String residenceType;
    private String status;
    private LocalDate stayStartDate;
    private LocalDate stayEndDate;
    private Integer isLocalHukou;
    private String proofType;
}
