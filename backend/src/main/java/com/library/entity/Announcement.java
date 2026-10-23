package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告(管理端发布 -> 读者端展示)
 */
@Data
@TableName("library_announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 发布人姓名 */
    private String publisherName;

    /** 1 已发布 */
    private Integer status;

    /** 是否置顶(可多条, 置顶的按时间倒序排前面) */
    private Boolean isPinned;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
