package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色
 */
@Data
@TableName("library_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;
}
