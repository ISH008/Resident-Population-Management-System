package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.SysRole;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.ChangePasswordRequest;
import com.population.resident.dto.LoginRequest;
import com.population.resident.dto.RegisterRequest;
import com.population.resident.dto.UpdateProfileRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.SysRoleMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.mapper.SysUserRoleMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.JwtService;
import com.population.resident.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void login_shouldReturnTokenAndUserInfo() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("user_01");
        user.setPassword(passwordEncoder.encode("123456"));
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setResidentId(8L);
        user.setResidentName("张三");
        when(sysUserMapper.findByUsername("user_01")).thenReturn(user);
        when(sysRoleMapper.findRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(jwtService.generateToken(1L, "user_01", List.of("USER"), "USER")).thenReturn("token-x");
        when(jwtService.getExpireSeconds()).thenReturn(7200L);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user_01");
        loginRequest.setPassword("123456");
        var response = authService.login(loginRequest);

        assertEquals("token-x", response.getToken());
        assertEquals(8L, response.getUserInfo().getResidentId());
        assertEquals("USER", response.getUserInfo().getCurrentRole());
    }

    @Test
    void login_shouldRejectLegacyPlainPassword() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("legacy_user");
        user.setPassword("123456");
        user.setStatus(1);
        user.setIsDeleted(0);
        when(sysUserMapper.findByUsername("legacy_user")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("legacy_user");
        request.setPassword("123456");

        BizException ex = assertThrows(BizException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void register_shouldCreateUserAndBindUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("中文_user");
        request.setPassword("123456");
        request.setRealName("王小明");
        request.setPhone("13800000000");
        when(sysUserMapper.findByUsername("中文_user")).thenReturn(null);
        SysRole role = new SysRole();
        role.setId(2L);
        role.setRoleCode("USER");
        when(sysRoleMapper.findByRoleCode("USER")).thenReturn(role);
        doAnswer(invocation -> {
            SysUser u = invocation.getArgument(0);
            u.setId(99L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        Long id = authService.register(request);

        assertEquals(99L, id);
        verify(sysUserRoleMapper).insert(99L, 2L);
    }

    @Test
    void changePassword_shouldRejectWrongOldPassword() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u")
                .currentRole("USER")
                .roles(List.of("USER"))
                .build());
        SysUser user = new SysUser();
        user.setId(10L);
        user.setPassword(passwordEncoder.encode("abc123"));
        user.setIsDeleted(0);
        when(sysUserMapper.findById(10L)).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("newPass1");

        BizException ex = assertThrows(BizException.class, () -> authService.changePassword(request));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void updateProfile_shouldRejectDuplicateUsername() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u")
                .currentRole("USER")
                .roles(List.of("USER"))
                .build());
        SysUser current = new SysUser();
        current.setId(10L);
        current.setIsDeleted(0);
        when(sysUserMapper.findById(10L)).thenReturn(current);
        SysUser exists = new SysUser();
        exists.setId(11L);
        when(sysUserMapper.findByUsername("重复名")).thenReturn(exists);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("重复名");
        request.setRealName("A");
        request.setPhone("13800000000");

        BizException ex = assertThrows(BizException.class, () -> authService.updateProfile(request));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode());
    }

    @Test
    void updateProfile_shouldPersistUsernameRealNamePhone() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u")
                .currentRole("USER")
                .roles(List.of("USER"))
                .build());
        SysUser current = new SysUser();
        current.setId(10L);
        current.setIsDeleted(0);
        when(sysUserMapper.findById(10L)).thenReturn(current);
        when(sysUserMapper.findByUsername("新用户名")).thenReturn(null);
        when(sysUserMapper.updateProfile(anyLong(), anyString(), anyString(), anyString())).thenReturn(1);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("新用户名");
        request.setRealName("新姓名");
        request.setPhone("13800000000");

        authService.updateProfile(request);

        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        verify(sysUserMapper).updateProfile(anyLong(), usernameCaptor.capture(), anyString(), anyString());
        assertEquals("新用户名", usernameCaptor.getValue());
    }
}
