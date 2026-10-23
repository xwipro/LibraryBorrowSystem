package com.library.dto.borrow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 办理借阅请求
 */
@Data
public class BorrowCreateRequest {

    @NotNull(message = "读者ID不能为空")
    private Long readerId;

    @NotNull(message = "图书ID不能为空")
    private Long bookId;
}
