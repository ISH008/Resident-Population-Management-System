package com.population.resident.mapper;

import com.population.resident.domain.ResidentJudgeRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResidentJudgeRuleMapper {
    List<ResidentJudgeRule> findEnabledRules(@Param("version") String version);
}
