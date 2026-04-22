package com.population.resident.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserPageItem {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private Long residentId;
    private String residentName;
    private Integer status;
    private List<String> roles;
    private LocalDateTime createdAt;
}
