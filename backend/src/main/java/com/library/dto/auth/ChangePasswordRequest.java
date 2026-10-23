package com.library.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人中心 - 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "请输入旧密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    private String newPassword;
}
