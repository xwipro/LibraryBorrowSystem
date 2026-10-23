package com.library.dto.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQuery extends PageQuery {

    private Long roleId;

    /** 0 禁用 / 1 启用 */
    private Integer status;
}
