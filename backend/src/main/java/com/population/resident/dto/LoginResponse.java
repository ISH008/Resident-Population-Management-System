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
        private List<String> roles;
        private String currentRole;
    }
}
