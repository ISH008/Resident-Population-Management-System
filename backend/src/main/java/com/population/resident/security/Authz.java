package com.population.resident.security;

import com.population.resident.common.ErrorCode;
import com.population.resident.exception.BizException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class Authz {

    private Authz() {
    }

    public static void requireAnyRole(String... expectedRoles) {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Set<String> owned = currentUser.getRoles().stream().map(String::toUpperCase).collect(Collectors.toSet());
        boolean matched = Arrays.stream(expectedRoles).map(String::toUpperCase).anyMatch(owned::contains);
        if (!matched) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }
}
