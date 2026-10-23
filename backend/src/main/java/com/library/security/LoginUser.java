package com.library.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 认证上下文中的登录用户信息
 */
@Data
@AllArgsConstructor
public class LoginUser {

    private Long userId;

    private String username;

    private String roleCode;

    private String realName;
}
