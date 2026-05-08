package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.Resident;
import com.population.resident.dto.JudgeRequest;
import com.population.resident.dto.JudgeResultResponse;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.ResidentUpsertRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeLogMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.security.CurrentUser;
import com.population.resident.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResidentServiceTest {

    @Mock
    private ResidentMapper residentMapper;
    @Mock
    private ResidentJudgeLogMapper residentJudgeLogMapper;

    @InjectMocks
    private ResidentService residentService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void pageQuery_shouldNormalizeInvalidPaginationArgs() {
        when(residentMapper.pageQuery(anyInt(), anyInt(), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of());
        when(residentMapper.count(nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(0L);

        PageResponse<Resident> page = residentService.pageQuery(0, 1000, null, null, null);

        assertEquals(1, page.getPageNum());
        assertEquals(100, page.getPageSize());
        verify(residentMapper).pageQuery(0, 100, null, null, null);
    }

    @Test
    void create_shouldFailWhenNoCurrentUserInContext() {
        ResidentUpsertRequest request = new ResidentUpsertRequest();
        request.setName("测试");
        request.setIdCard("11010519491231002X");
        request.setGender("M");
        request.setStayStartDate(LocalDate.now().minusDays(10));

        BizException ex = assertThrows(BizException.class, () -> residentService.create(request));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void judge_shouldReturnNonResidentWhenExcludeRuleHit() {
        UserContext.set(CurrentUser.builder()
                .userId(1L)
                .username("admin")
                .roles(List.of("ADMIN"))
                .currentRole("ADMIN")
                .build());

        Resident resident = new Resident();
        resident.setId(1L);
        when(residentMapper.findById(1L)).thenReturn(resident);
        when(residentMapper.updateJudgeResult(eq(1L), eq("NON_RESIDENT"), eq(0), eq("v2"), anyString(), eq(1L))).thenReturn(1);

        JudgeRequest request = new JudgeRequest();
        request.setTemporaryVisitorOnSurveyNight(true);

        JudgeResultResponse result = residentService.judge(1L, request);
        assertEquals("NON_RESIDENT", result.getFinalStatus());
        assertEquals("v2", result.getJudgeVersion());
        verify(residentJudgeLogMapper).insert(any());
    }

    @Test
    void judge_shouldReturnPendingWhenNoRuleMatched() {
        UserContext.set(CurrentUser.builder()
                .userId(1L)
                .username("admin")
                .roles(List.of("ADMIN"))
                .currentRole("ADMIN")
                .build());

        Resident resident = new Resident();
        resident.setId(1L);
        when(residentMapper.findById(1L)).thenReturn(resident);
        when(residentMapper.updateJudgeResult(eq(1L), eq("PENDING"), eq(0), eq("v2"), anyString(), eq(1L))).thenReturn(1);

        var result = residentService.judge(1L, new JudgeRequest());

        assertEquals("PENDING", result.getFinalStatus());
        assertEquals(0, result.getFinalScore());
        verify(residentJudgeLogMapper).insert(any());
    }
}
