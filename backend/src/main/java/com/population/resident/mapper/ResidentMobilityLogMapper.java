package com.population.resident.mapper;

import com.population.resident.domain.ResidentMobilityLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResidentMobilityLogMapper {

    int insert(ResidentMobilityLog log);

    ResidentMobilityLog findById(@Param("id") Long id);

    List<ResidentMobilityLog> findByResidentId(@Param("residentId") Long residentId);

    int deleteById(@Param("id") Long id);
}

