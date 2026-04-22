package com.population.resident.security;

import com.population.resident.common.ErrorCode;
import com.population.resident.exception.BizException;

import java.util.Arrays;

public final class Authz {

    private Authz() {
    }

    public static void requireAnyRole(String... expectedRoles) {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        String currentRole = currentUser.getCurrentRole();
        if (currentRole == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        boolean matched = Arrays.stream(expectedRoles)
                .map(String::toUpperCase)
                .anyMatch(role -> role.equals(currentRole.toUpperCase()));
        if (!matched) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }
}
