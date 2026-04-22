package com.population.resident.mapper;

import com.population.resident.domain.ResidentJudgeApplicationAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResidentJudgeApplicationAttachmentMapper {
    int insert(ResidentJudgeApplicationAttachment attachment);

    List<ResidentJudgeApplicationAttachment> findByApplicationId(@Param("applicationId") Long applicationId);

    ResidentJudgeApplicationAttachment findById(@Param("id") Long id);
}
