package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.LoginRequest;
import com.population.resident.dto.LoginResponse;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.SysRoleMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.JwtService;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.findByUsername(request.getUsername());
        if (user == null || user.getIsDeleted() == 1 || user.getStatus() == 0) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        boolean passwordOk = passwordEncoder.matches(request.getPassword(), user.getPassword())
                || request.getPassword().equals(user.getPassword());
        if (!passwordOk) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        List<String> roles = sysRoleMapper.findRoleCodesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "用户未分配角色");
        }
        String currentRole = roles.get(0);
        String token = jwtService.generateToken(user.getId(), user.getUsername(), roles, currentRole);

        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpireSeconds())
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .roles(roles)
                        .currentRole(currentRole)
                        .build())
                .build();
    }

    public LoginResponse.UserInfo me() {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return LoginResponse.UserInfo.builder()
                .id(currentUser.getUserId())
                .username(currentUser.getUsername())
                .roles(currentUser.getRoles())
                .currentRole(currentUser.getCurrentRole())
                .build();
    }

    public LoginResponse switchRole(String targetRole) {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(targetRole)) {
            throw new BizException(ErrorCode.BAD_REQUEST);
        }
        boolean owned = currentUser.getRoles().stream().anyMatch(targetRole::equalsIgnoreCase);
        if (!owned) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "目标角色未分配");
        }

        String finalRole = targetRole.toUpperCase();
        String token = jwtService.generateToken(
                currentUser.getUserId(),
                currentUser.getUsername(),
                currentUser.getRoles(),
                finalRole
        );
        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpireSeconds())
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(currentUser.getUserId())
                        .username(currentUser.getUsername())
                        .roles(currentUser.getRoles())
                        .currentRole(finalRole)
                        .build())
                .build();
    }
}
