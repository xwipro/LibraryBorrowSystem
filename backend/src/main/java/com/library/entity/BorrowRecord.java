package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅记录
 */
@Data
@TableName("library_borrow_record")
public class BorrowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Long bookId;

    private LocalDateTime borrowTime;

    private LocalDateTime dueTime;

    private LocalDateTime returnTime;

    /** 0 借阅中 / 1 已归还 / 2 已逾期 / 3 已预约 */
    private Integer status;

    private Integer renewCount;

    /** 读者姓名(联表返回, 非物理列) */
    @TableField(exist = false)
    private String readerName;

    /** 书名(联表返回, 非物理列) */
    @TableField(exist = false)
    private String bookTitle;

    /** ISBN(联表返回, 非物理列) */
    @TableField(exist = false)
    private String isbn;
}
