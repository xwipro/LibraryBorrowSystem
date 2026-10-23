package com.library.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应 { list, total, pageNum, pageSize }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long pageNum;
    private long pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }
}
