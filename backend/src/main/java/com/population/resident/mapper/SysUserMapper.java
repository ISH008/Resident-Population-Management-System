package com.population.resident.mapper;

import com.population.resident.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper {
    SysUser findByUsername(@Param("username") String username);

    List<SysUser> pageQuery(@Param("offset") Integer offset,
                            @Param("pageSize") Integer pageSize,
                            @Param("username") String username,
                            @Param("realName") String realName,
                            @Param("status") Integer status);

    Long count(@Param("username") String username,
               @Param("realName") String realName,
               @Param("status") Integer status);

    SysUser findById(@Param("id") Long id);

    int insert(SysUser user);

    int updateById(SysUser user);

    int logicalDelete(@Param("id") Long id);
}
