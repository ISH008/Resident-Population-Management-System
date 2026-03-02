package com.population.resident.mapper;

import com.population.resident.domain.Resident;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResidentMapper {
    List<Resident> pageQuery(@Param("offset") Integer offset,
                             @Param("pageSize") Integer pageSize,
                             @Param("name") String name,
                             @Param("idCard") String idCard,
                             @Param("residenceStatus") String residenceStatus);

    Long count(@Param("name") String name,
               @Param("idCard") String idCard,
               @Param("residenceStatus") String residenceStatus);

    Resident findById(@Param("id") Long id);

    int insert(Resident resident);

    int updateById(Resident resident);

    int logicalDelete(@Param("id") Long id, @Param("updatedBy") Long updatedBy);

    int updateJudgeResult(@Param("id") Long id,
                          @Param("residenceStatus") String residenceStatus,
                          @Param("residenceScore") Integer residenceScore,
                          @Param("judgeVersion") String judgeVersion,
                          @Param("judgeReason") String judgeReason,
                          @Param("updatedBy") Long updatedBy);

    int updateManualJudge(@Param("id") Long id,
                          @Param("residenceStatus") String residenceStatus,
                          @Param("judgeReason") String judgeReason,
                          @Param("updatedBy") Long updatedBy);
}
