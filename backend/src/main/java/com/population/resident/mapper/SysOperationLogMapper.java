package com.population.resident.mapper;

import com.population.resident.domain.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysOperationLogMapper {
    int insert(SysOperationLog log);

    List<SysOperationLog> pageQuery(@Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize,
                                    @Param("module") String module,
                                    @Param("result") String result);

    Long count(@Param("module") String module, @Param("result") String result);
}
