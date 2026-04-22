package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.SysRole;
import com.population.resident.domain.SysUser;
import com.population.resident.dto.PageResponse;
import com.population.resident.dto.UserCreateRequest;
import com.population.resident.dto.UserPageItem;
import com.population.resident.dto.UserUpdateRequest;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.SysRoleMapper;
import com.population.resident.mapper.ResidentMapper;
import com.population.resident.mapper.SysUserMapper;
import com.population.resident.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final ResidentMapper residentMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PageResponse<UserPageItem> pageQuery(Integer pageNum, Integer pageSize, String username, String realName, Integer status) {
        int validPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int validPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (validPageNum - 1) * validPageSize;

        List<SysUser> users = sysUserMapper.pageQuery(offset, validPageSize, username, realName, status);
        List<UserPageItem> items = users.stream().map(u -> UserPageItem.builder()
                .id(u.getId())
                .username(u.getUsername())
                .realName(u.getRealName())
                .phone(u.getPhone())
                .residentId(u.getResidentId())
                .residentName(u.getResidentName())
                .status(u.getStatus())
                .roles(sysRoleMapper.findRoleCodesByUserId(u.getId()))
                .createdAt(u.getCreatedAt())
                .build()).toList();
        Long total = sysUserMapper.count(username, realName, status);

        return PageResponse.<UserPageItem>builder()
                .total(total)
                .pageNum(validPageNum)
                .pageSize(validPageSize)
                .records(items)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        validateRoles(request.getRoleIds());
        validateResidentBinding(request.getResidentId(), null);
        SysUser exists = sysUserMapper.findByUsername(request.getUsername());
        if (exists != null) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setResidentId(request.getResidentId());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        sysUserMapper.insert(user);

        for (Long roleId : request.getRoleIds()) {
            sysUserRoleMapper.insert(user.getId(), roleId);
        }
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateRequest request) {
        validateRoles(request.getRoleIds());
        validateResidentBinding(request.getResidentId(), id);
        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        user.setId(id);
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setResidentId(request.getResidentId());
        user.setStatus(request.getStatus() == null ? user.getStatus() : request.getStatus());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserMapper.updateById(user);

        sysUserRoleMapper.deleteByUserId(id);
        for (Long roleId : request.getRoleIds()) {
            sysUserRoleMapper.insert(id, roleId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        int updated = sysUserMapper.logicalDelete(id);
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        sysUserRoleMapper.deleteByUserId(id);
    }

    public List<SysRole> listRoles() {
        return sysRoleMapper.findAllEnabled();
    }

    private void validateRoles(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            if (sysRoleMapper.countByRoleId(roleId) == 0) {
                throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "角色不存在: " + roleId);
            }
        }
    }

    private void validateResidentBinding(Long residentId, Long currentUserId) {
        if (residentId == null) {
            return;
        }
        if (residentMapper.findById(residentId) == null) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "绑定居民不存在");
        }
        SysUser boundUser = sysUserMapper.findByResidentId(residentId);
        if (boundUser != null && (currentUserId == null || !boundUser.getId().equals(currentUserId))) {
            throw new BizException(ErrorCode.CONFLICT.getCode(), "该居民已绑定其他账号");
        }
    }
}
