package com.population.resident.security;

import com.population.resident.common.ErrorCode;
import com.population.resident.exception.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorization.substring(7);
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = claims.get("uid", Long.class);
        String username = claims.getSubject();
        String currentRole = claims.get("currentRole", String.class);
        List<String> roles = claims.get("roles", List.class);

        UserContext.set(CurrentUser.builder()
                .userId(userId)
                .username(username)
                .currentRole(currentRole)
                .roles(roles == null ? Collections.emptyList() : roles)
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
