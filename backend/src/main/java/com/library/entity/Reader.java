package com.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 读者
 */
@Data
@TableName("library_reader")
public class Reader {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String readerNo;

    private String name;

    /** 0 男 / 1 女 */
    private Integer gender;

    private String phone;

    private String email;

    private String avatar;

    /** 学生 / 教师 / 职工 / 校外 */
    private String readerType;

    private Integer borrowCount;

    private Integer maxBorrow;

    /** 0 禁用 / 1 正常 */
    private Integer status;

    private LocalDateTime createTime;
}
