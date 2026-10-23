package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.query.LogQuery;
import com.library.dto.query.SysUserQuery;
import com.library.entity.SysLog;
import com.library.entity.SysRole;
import com.library.entity.SysUser;
import com.library.service.RoleService;
import com.library.service.SysLogService;
import com.library.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统接口: 用户 / 角色 / 操作日志
 */
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final SysUserService sysUserService;
    private final SysLogService sysLogService;
    private final RoleService roleService;

    // ===== 系统用户 =====

    /** 用户分页查询 */
    @GetMapping("/user/page")
    public Result<PageResult<SysUser>> userPage(SysUserQuery query) {
        return Result.ok(sysUserService.page(query));
    }

    /** 新增用户 */
    @PostMapping("/user")
    public Result<SysUser> userCreate(@RequestBody SysUser user,
                                      Authentication auth, HttpServletRequest request) {
        SysUser created = sysUserService.create(user);
        sysLogService.log(auth.getName(), "系统设置", "新增用户", clientIp(request), 1);
        return Result.ok(created);
    }

    /** 更新用户 */
    @PutMapping("/user/{id}")
    public Result<SysUser> userUpdate(@PathVariable Long id, @RequestBody SysUser user,
                                      Authentication auth, HttpServletRequest request) {
        SysUser updated = sysUserService.update(id, user);
        sysLogService.log(auth.getName(), "系统设置", "修改用户", clientIp(request), 1);
        return Result.ok(updated);
    }

    /** 删除用户 */
    @DeleteMapping("/user/{id}")
    public Result<Boolean> userRemove(@PathVariable Long id,
                                      Authentication auth, HttpServletRequest request) {
        sysUserService.remove(id);
        sysLogService.log(auth.getName(), "系统设置", "删除用户", clientIp(request), 1);
        return Result.ok(true);
    }

    /**
     * 重置用户密码:
     * - 超级管理员: body 必须携带 newPassword(由超管本人输入), 直接生效
     * - 其他用户: 随机生成密码并通过邮件发送给用户, 密码不返回
     */
    @PutMapping("/user/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body,
                                      Authentication auth, HttpServletRequest request) {
        String newPassword = body == null ? null : body.get("newPassword");
        sysUserService.resetPassword(id, newPassword);
        sysLogService.log(auth.getName(), "系统设置", "重置密码", clientIp(request), 1);
        return Result.ok();
    }

    // ===== 操作日志 =====

    /** 操作日志分页查询(日志为审计记录, 只读, 不可删除) */
    @GetMapping("/log/page")
    public Result<PageResult<SysLog>> logPage(LogQuery query) {
        return Result.ok(sysLogService.page(query));
    }

    // ===== 角色 =====

    /** 角色列表 */
    @GetMapping("/role/list")
    public Result<List<SysRole>> roleList() {
        return Result.ok(roleService.list());
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
