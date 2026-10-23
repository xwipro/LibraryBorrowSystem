package com.library.service;

import com.library.entity.SysRole;
import com.library.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper sysRoleMapper;

    public List<SysRole> list() {
        return sysRoleMapper.selectList(null);
    }
}
