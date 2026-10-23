package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 读者分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReaderQuery extends PageQuery {

    private String readerType;

    /** 0 禁用 / 1 正常 */
    private Integer status;
}
