package com.library.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布公告请求
 */
@Data
public class AnnouncementCreateRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过 200 字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;
}
