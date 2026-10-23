package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书分类
 */
@Data
@TableName("library_book_category")
public class BookCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
