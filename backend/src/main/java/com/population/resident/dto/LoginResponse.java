package com.population.resident.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long expiresIn;
    private UserInfo userInfo;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private Long residentId;
        private String residentName;
        private List<String> roles;
        private String currentRole;
    }
}
