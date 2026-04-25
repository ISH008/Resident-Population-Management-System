package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.SysRole;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.ChangePasswordRequest;
import com.population.resident.dto.LoginRequest;
import com.population.resident.dto.LoginResponse;
import com.population.resident.dto.RegisterRequest;
import com.population.resident.dto.UpdateProfileRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.SysRoleMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.mapper.SysUserRoleMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.JwtService;
import com.population.resident.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.findByUsername(request.getUsername());
        if (user == null || user.getIsDeleted() == 1 || user.getStatus() == 0) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        boolean passwordOk = passwordEncoder.matches(request.getPassword(), user.getPassword());
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
                        .realName(user.getRealName())
                        .phone(user.getPhone())
                        .residentId(user.getResidentId())
                        .residentName(user.getResidentName())
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
        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return LoginResponse.UserInfo.builder()
                .id(currentUser.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .residentId(user.getResidentId())
                .residentName(user.getResidentName())
                .roles(currentUser.getRoles())
                .currentRole(currentUser.getCurrentRole())
                .build();
    }

    public LoginResponse switchRole(String targetRole) {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
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
                user.getUsername(),
                currentUser.getRoles(),
                finalRole
        );
        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpireSeconds())
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(currentUser.getUserId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .phone(user.getPhone())
                        .residentId(user.getResidentId())
                        .residentName(user.getResidentName())
                        .roles(currentUser.getRoles())
                        .currentRole(finalRole)
                        .build())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        SysUser exists = sysUserMapper.findByUsername(request.getUsername());
        if (exists != null) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "用户名已存在");
        }
        SysRole userRole = sysRoleMapper.findByRoleCode("USER");
        if (userRole == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(), "系统未配置USER角色");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        user.setResidentId(null);
        sysUserMapper.insert(user);
        sysUserRoleMapper.insert(user.getId(), userRole.getId());
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        boolean oldPasswordOk = passwordEncoder.matches(request.getOldPassword(), user.getPassword());
        if (!oldPasswordOk) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "旧密码错误");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "新旧密码不能相同");
        }
        int updated = sysUserMapper.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
        if (updated == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileRequest request) {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = sysUserMapper.findById(currentUser.getUserId());
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "username不能为空");
        }
        SysUser exists = sysUserMapper.findByUsername(request.getUsername());
        if (exists != null && !exists.getId().equals(user.getId())) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "用户名已存在");
        }

        int updated = sysUserMapper.updateProfile(
                user.getId(),
                request.getUsername(),
                request.getRealName(),
                request.getPhone()
        );
        if (updated == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
