package com.library.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 { token }
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
}
