package com.population.resident.mapper;

import com.population.resident.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    List<SysRole> findAllEnabled();

    int countByRoleId(@Param("roleId") Long roleId);

    SysRole findByRoleCode(@Param("roleCode") String roleCode);
}
