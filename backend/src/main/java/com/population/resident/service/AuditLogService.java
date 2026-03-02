package com.population.resident.service;

import com.population.resident.domain.SysOperationLog;
import com.population.resident.dto.PageResponse;
import com.population.resident.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public void save(SysOperationLog log) {
        sysOperationLogMapper.insert(log);
    }

    public PageResponse<SysOperationLog> pageQuery(Integer pageNum, Integer pageSize, String module, String result) {
        int validPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (validPageNum - 1) * validPageSize;
        List<SysOperationLog> records = sysOperationLogMapper.pageQuery(offset, validPageSize, module, result);
        Long total = sysOperationLogMapper.count(module, result);
        return PageResponse.<SysOperationLog>builder()
                .total(total)
                .pageNum(validPageNum)
                .pageSize(validPageSize)
                .records(records)
                .build();
    }
}
