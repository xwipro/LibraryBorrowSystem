package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@TableName("library_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String module;

    private String action;

    private String ip;

    private LocalDateTime time;

    /** 0 失败 / 1 成功 */
    private Integer status;
}
