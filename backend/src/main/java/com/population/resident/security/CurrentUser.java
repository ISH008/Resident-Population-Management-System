package com.population.resident.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CurrentUser {
    private Long userId;
    private String username;
    private List<String> roles;
    private String currentRole;
}
