package com.library.dto.query;

import lombok.Data;

/**
 * 通用分页查询基类
 */
@Data
public class PageQuery {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;
}
