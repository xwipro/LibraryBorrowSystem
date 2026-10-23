package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.BusinessException;
import com.library.dto.auth.*;
import com.library.entity.SysRole;
import com.library.entity.SysUser;
import com.library.mapper.SysRoleMapper;
import com.library.mapper.SysUserMapper;
import com.library.security.JwtUtil;
import com.library.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证服务: 登录 / 登出 / 当前用户信息 / 找回密码
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String FORGOT_PREFIX = "forgot:";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final CaptchaService captchaService;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.forgot-expire-seconds:300}")
    private long forgotExpireSeconds;

    /** 登录: 验证码 -> 用户 -> 密码 -> 签发 token */
    public LoginResponse login(LoginRequest req) {
        if (!captchaService.verify(req.getCaptchaKey(), req.getCaptcha())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String roleCode = resolveRoleCode(user.getRoleId());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleCode);

        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return new LoginResponse(token);
    }

    /** 登出: 将当前 token 拉入黑名单(剩余有效期) */
    public void logout() {
        Claims claims = currentClaims();
        if (claims != null) {
            long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            blacklistService.blacklist(claims.getId(), Math.max(1, ttlMillis / 1000));
        }
    }

    /** 当前登录用户信息 */
    public Map<String, Object> info() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null ? null : auth.getName();
        if (username == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        String roleCode = resolveRoleCode(user.getRoleId());
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());
        info.put("roles", List.of(roleCode));
        info.put("permissions", List.of("*"));
        return info;
    }

    /** 获取登录验证码 */
    public CaptchaResult captcha() {
        return captchaService.generate();
    }

    /** 找回密码: 发送验证码到邮箱(异步, 不等待) */
    public void forgotPassword(String email) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            // 不暴露邮箱是否存在, 统一返回成功
            return;
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set(FORGOT_PREFIX + email, code,
                Duration.ofSeconds(forgotExpireSeconds));
        mailService.sendVerifyCode(email, code);
    }

    /** 找回密码: 校验验证码并重置(合规后立即删除 redis 记录) */
    public void resetPassword(ResetPasswordRequest req) {
        String cached = redisTemplate.opsForValue().get(FORGOT_PREFIX + req.getEmail());
        redisTemplate.delete(FORGOT_PREFIX + req.getEmail());
        if (cached == null || !cached.equals(req.getCode().trim())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, req.getEmail()));
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    /** 个人中心 - 修改自己的密码(校验旧密码) */
    public void changePassword(ChangePasswordRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null ? null : auth.getName();
        if (username == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    private String resolveRoleCode(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        return role == null ? "" : role.getCode();
    }

    private Claims currentClaims() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof Claims claims)) {
            return null;
        }
        return claims;
    }
}
