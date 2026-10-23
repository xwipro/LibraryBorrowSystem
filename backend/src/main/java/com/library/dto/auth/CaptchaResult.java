package com.library.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码响应 { key, img }
 */
@Data
@AllArgsConstructor
public class CaptchaResult {

    private String key;

    private String img;
}
