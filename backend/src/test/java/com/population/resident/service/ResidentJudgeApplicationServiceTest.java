package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.dto.PageResponse;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeApplicationMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResidentJudgeApplicationServiceTest {

    @Mock
    private ResidentJudgeApplicationMapper residentJudgeApplicationMapper;
    @Mock
    private ResidentMapper residentMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private ResidentService residentService;
    @Mock
    private JudgeApplicationPermissionService permissionService;

    private ResidentJudgeApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ResidentJudgeApplicationService(
                residentJudgeApplicationMapper,
                residentMapper,
                sysUserMapper,
                residentService,
                permissionService
        );
        when(permissionService.requireCurrentUser()).thenAnswer(invocation -> UserContext.get());
        when(permissionService.isUserRole(any())).thenAnswer(invocation -> {
            CurrentUser currentUser = invocation.getArgument(0);
            return currentUser != null && "USER".equalsIgnoreCase(currentUser.getCurrentRole());
        });
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void pageMine_shouldApplyResidentBoundFilterForUserRole() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u1")
                .roles(List.of("USER"))
                .currentRole("USER")
                .build());
        com.population.resident.domain.SysUser dbUser = new com.population.resident.domain.SysUser();
        dbUser.setId(10L);
        dbUser.setResidentId(8L);
        when(sysUserMapper.findById(10L)).thenReturn(dbUser);
        when(residentJudgeApplicationMapper.pageMine(anyInt(), anyInt(), anyLong(), anyString(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(residentJudgeApplicationMapper.countMine(anyLong(), anyString(), anyLong())).thenReturn(0L);

        PageResponse<ResidentJudgeApplication> page = service.pageMine(1, 10, "PENDING");

        assertEquals(0L, page.getTotal());
        verify(residentJudgeApplicationMapper).pageMine(0, 10, 10L, "PENDING", 8L);
    }

    @Test
    void pageMine_shouldReturnEmptyWhenUserNotBoundToResident() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u1")
                .roles(List.of("USER"))
                .currentRole("USER")
                .build());
        com.population.resident.domain.SysUser dbUser = new com.population.resident.domain.SysUser();
        dbUser.setId(10L);
        dbUser.setResidentId(null);
        when(sysUserMapper.findById(10L)).thenReturn(dbUser);

        PageResponse<ResidentJudgeApplication> page = service.pageMine(1, 10, "");

        assertEquals(0L, page.getTotal());
        assertEquals(0, page.getRecords().size());
    }

    @Test
    void pageMine_shouldNotApplyResidentFilterForAdminRole() {
        UserContext.set(CurrentUser.builder()
                .userId(1L)
                .username("admin")
                .roles(List.of("ADMIN"))
                .currentRole("ADMIN")
                .build());
        when(residentJudgeApplicationMapper.pageMine(anyInt(), anyInt(), anyLong(), anyString(), isNull()))
                .thenReturn(Collections.emptyList());
        when(residentJudgeApplicationMapper.countMine(anyLong(), anyString(), isNull())).thenReturn(0L);

        service.pageMine(1, 10, "");

        verify(residentJudgeApplicationMapper).pageMine(0, 10, 1L, "", null);
    }

    @Test
    void create_shouldRejectWhenUserTriesNonBoundResident() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u1")
                .roles(List.of("USER"))
                .currentRole("USER")
                .build());
        com.population.resident.domain.SysUser dbUser = new com.population.resident.domain.SysUser();
        dbUser.setId(10L);
        dbUser.setResidentId(8L);
        when(sysUserMapper.findById(10L)).thenReturn(dbUser);

        var request = new com.population.resident.dto.JudgeApplicationCreateRequest();
        request.setResidentId(7L);
        request.setApplyReason("x");

        BizException ex = assertThrows(BizException.class, () -> service.create(request));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void create_shouldUseBoundResidentIdForUser() {
        UserContext.set(CurrentUser.builder()
                .userId(10L)
                .username("u1")
                .roles(List.of("USER"))
                .currentRole("USER")
                .build());
        com.population.resident.domain.SysUser dbUser = new com.population.resident.domain.SysUser();
        dbUser.setId(10L);
        dbUser.setResidentId(8L);
        when(sysUserMapper.findById(10L)).thenReturn(dbUser);
        Resident resident = new Resident();
        resident.setId(8L);
        when(residentMapper.findById(8L)).thenReturn(resident);
        when(residentJudgeApplicationMapper.insert(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            ResidentJudgeApplication a = inv.getArgument(0);
            a.setId(100L);
            return 1;
        });

        var request = new com.population.resident.dto.JudgeApplicationCreateRequest();
        request.setApplyReason("申请原因");

        Long id = service.create(request);
        assertEquals(100L, id);
    }
}
