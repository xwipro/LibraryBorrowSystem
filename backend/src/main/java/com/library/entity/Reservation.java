package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约记录
 */
@Data
@TableName("library_reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Long bookId;

    private LocalDateTime reserveTime;

    private LocalDateTime expireTime;

    /** 0 待处理 / 1 已通知 / 2 已取消 */
    private Integer status;

    /** 读者姓名(联表返回, 非物理列) */
    @TableField(exist = false)
    private String readerName;

    /** 书名(联表返回, 非物理列) */
    @TableField(exist = false)
    private String bookTitle;
}
