package com.population.resident.mapper;

import com.population.resident.domain.ResidentJudgeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ResidentJudgeLogMapper {
    int insert(ResidentJudgeLog log);

    List<ResidentJudgeLog> findByResidentId(Long residentId);
}
