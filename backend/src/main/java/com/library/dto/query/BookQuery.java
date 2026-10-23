package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图书分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BookQuery extends PageQuery {

    private Long categoryId;

    /** 0 下架 / 1 在架 */
    private Integer status;
}
