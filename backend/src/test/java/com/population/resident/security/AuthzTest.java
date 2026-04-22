package com.population.resident.security;

import com.population.resident.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthzTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void requireAnyRole_shouldPass_whenCurrentRoleMatches() {
        UserContext.set(CurrentUser.builder()
                .userId(1L)
                .username("u")
                .currentRole("USER")
                .roles(List.of("USER", "ADMIN"))
                .build());

        assertDoesNotThrow(() -> Authz.requireAnyRole("USER"));
    }

    @Test
    void requireAnyRole_shouldFail_whenCurrentRoleDoesNotMatch() {
        UserContext.set(CurrentUser.builder()
                .userId(1L)
                .username("u")
                .currentRole("USER")
                .roles(List.of("USER", "ADMIN"))
                .build());

        assertThrows(BizException.class, () -> Authz.requireAnyRole("ADMIN"));
    }
}

