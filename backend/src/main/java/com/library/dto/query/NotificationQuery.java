package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQuery extends PageQuery {

    /** 类型筛选: borrow / system / overdue, 可空 */
    private String type;
}
