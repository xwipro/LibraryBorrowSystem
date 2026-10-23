package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.query.SysUserQuery;
import com.library.entity.SysRole;
import com.library.entity.SysUser;
import com.library.mapper.SysRoleMapper;
import com.library.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final String DEFAULT_PASSWORD = "123456";
    /** 超管角色编码: 该角色用户不可被删除 */
    private static final String SUPER_ADMIN_ROLE = "admin";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public PageResult<SysUser> page(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysUser::getUsername, query.getKeyword())
                        .or().like(SysUser::getRealName, query.getKeyword())
                        .or().like(SysUser::getPhone, query.getKeyword()))
                .eq(query.getRoleId() != null, SysUser::getRoleId, query.getRoleId())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        fillRole(page.getRecords());
        // 密码不对外返回
        page.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public SysUser create(SysUser user) {
        Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        user.setPassword(null);
        fillRole(List.of(user));
        return user;
    }

    @Transactional
    public SysUser update(Long id, SysUser user) {
        if (sysUserMapper.selectById(id) == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 密码不允许通过普通更新接口修改
        user.setId(id);
        user.setPassword(null);
        sysUserMapper.updateById(user);
        SysUser updated = sysUserMapper.selectById(id);
        updated.setPassword(null);
        fillRole(List.of(updated));
        return updated;
    }

    @Transactional
    public void remove(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (isSuperAdmin(user)) {
            throw new BusinessException(400, "超级管理员不可删除");
        }
        sysUserMapper.deleteById(id);
    }

    /**
     * 重置密码:
     * - 超级管理员: 必须由本人输入新密码(newPassword 必传), 直接生效
     * - 其他用户: 随机生成强密码并发送至用户邮箱, 密码不对外返回
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (isSuperAdmin(user)) {
            if (!StringUtils.hasText(newPassword)) {
                throw new BusinessException(400, "超级管理员需输入新密码");
            }
            if (newPassword.length() < 6 || newPassword.length() > 32) {
                throw new BusinessException(400, "密码长度需在 6-32 位之间");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            sysUserMapper.updateById(user);
            return;
        }
        // 普通用户: 随机生成 + 邮件发送, 密码绝不返回
        String randomPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(randomPassword));
        sysUserMapper.updateById(user);
        mailService.sendResetPassword(user.getEmail(), user.getUsername(), randomPassword);
    }

    private boolean isSuperAdmin(SysUser user) {
        SysRole role = user.getRoleId() == null ? null : sysRoleMapper.selectById(user.getRoleId());
        return role != null && SUPER_ADMIN_ROLE.equals(role.getCode());
    }

    /** 生成 12 位随机强密码(大小写+数字, 排除易混淆字符) */
    private String generateRandomPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String all = upper + lower + digits;
        char[] arr = new char[12];
        arr[0] = upper.charAt(secureRandom.nextInt(upper.length()));
        arr[1] = lower.charAt(secureRandom.nextInt(lower.length()));
        arr[2] = digits.charAt(secureRandom.nextInt(digits.length()));
        for (int i = 3; i < arr.length; i++) {
            arr[i] = all.charAt(secureRandom.nextInt(all.length()));
        }
        // Fisher-Yates 洗牌, 避免固定格式
        for (int i = arr.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }

    private void fillRole(Collection<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, String> roleMap = sysRoleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getName));
        users.forEach(u -> u.setRoleName(roleMap.get(u.getRoleId())));
    }
}
