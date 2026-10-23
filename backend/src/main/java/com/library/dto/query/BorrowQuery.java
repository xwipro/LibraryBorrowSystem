package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 借阅记录分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BorrowQuery extends PageQuery {

    /** 0 借阅中 / 1 已归还 / 2 已逾期 / 3 已预约 */
    private Integer status;

    private Long readerId;
}
