package com.library.dto.borrow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 逾期处理请求
 */
@Data
public class OverdueRequest {

    @NotNull(message = "罚款金额不能为空")
    private Integer penalty;
}
