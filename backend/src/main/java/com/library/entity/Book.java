package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书
 */
@Data
@TableName("library_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String isbn;

    private String title;

    private String author;

    private String publisher;

    private LocalDate publishDate;

    private Long categoryId;

    /** 分类名称(联表返回, 非物理列) */
    @TableField(exist = false)
    private String categoryName;

    private String cover;

    private String summary;

    private Integer stockCount;

    private Integer totalCount;

    private String location;

    /** 0 下架 / 1 在架 */
    private Integer status;

    private BigDecimal rating;

    private Integer borrowCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
