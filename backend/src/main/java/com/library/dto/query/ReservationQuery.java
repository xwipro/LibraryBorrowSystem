package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约记录分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationQuery extends PageQuery {

    /** 0 待处理 / 1 已通知 / 2 已取消 */
    private Integer status;
}
