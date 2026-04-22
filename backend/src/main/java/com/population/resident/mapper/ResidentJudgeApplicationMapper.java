package com.population.resident.mapper;

import com.population.resident.domain.ResidentJudgeApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResidentJudgeApplicationMapper {
    int insert(ResidentJudgeApplication application);

    ResidentJudgeApplication findById(@Param("id") Long id);

    List<ResidentJudgeApplication> pageMine(@Param("offset") Integer offset,
                                            @Param("pageSize") Integer pageSize,
                                            @Param("applicantId") Long applicantId,
                                            @Param("status") String status,
                                            @Param("residentId") Long residentId);

    Long countMine(@Param("applicantId") Long applicantId,
                   @Param("status") String status,
                   @Param("residentId") Long residentId);

    List<ResidentJudgeApplication> pageAdmin(@Param("offset") Integer offset,
                                             @Param("pageSize") Integer pageSize,
                                             @Param("status") String status,
                                             @Param("residentName") String residentName,
                                             @Param("applicantUsername") String applicantUsername);

    Long countAdmin(@Param("status") String status,
                    @Param("residentName") String residentName,
                    @Param("applicantUsername") String applicantUsername);

    int updateReviewed(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("reviewComment") String reviewComment,
                       @Param("reviewerId") Long reviewerId);
}
