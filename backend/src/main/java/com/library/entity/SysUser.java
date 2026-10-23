package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户
 */
@Data
@TableName("library_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希, 不对外返回 */
    private String password;

    private String realName;

    private String avatar;

    private String phone;

    private String email;

    private Long roleId;

    /** 角色名称(联表返回, 非物理列) */
    @TableField(exist = false)
    private String roleName;

    /** 0 禁用 / 1 启用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;
}
