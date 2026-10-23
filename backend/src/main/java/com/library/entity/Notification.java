package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知(管理端通知中心, 按登录用户隔离)
 */
@Data
@TableName("library_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户名(通知只对该用户可见) */
    private String username;

    /** 类型: borrow=借阅 / system=系统 / overdue=逾期 */
    private String type;

    private String title;

    private String content;

    /** true=已读(不可逆) */
    private Boolean isRead;

    private LocalDateTime createTime;
}
