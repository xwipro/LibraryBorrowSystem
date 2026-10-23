package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogQuery extends PageQuery {

    private String module;
}
