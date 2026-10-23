package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.query.NotificationQuery;
import com.library.entity.Notification;
import com.library.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口(管理端通知中心): 列表 / 已读 / 删除 / 清空 / 未读数
 * 所有数据按当前登录用户名隔离
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 通知分页查询(本人) */
    @GetMapping("/page")
    public Result<PageResult<Notification>> page(NotificationQuery query, Authentication auth) {
        return Result.ok(notificationService.page(query, auth.getName()));
    }

    /** 未读数 */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(Authentication auth) {
        return Result.ok(notificationService.unreadCount(auth.getName()));
    }

    /** 标记单条已读(不可逆) */
    @PutMapping("/read/{id}")
    public Result<Boolean> markRead(@PathVariable Long id, Authentication auth) {
        return Result.ok(notificationService.markRead(id, auth.getName()));
    }

    /** 全部已读(不可逆) */
    @PutMapping("/read-all")
    public Result<Boolean> markAllRead(Authentication auth) {
        return Result.ok(notificationService.markAllRead(auth.getName()));
    }

    /** 删除单条通知 */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id, Authentication auth) {
        return Result.ok(notificationService.remove(id, auth.getName()));
    }

    /** 清空全部通知 */
    @DeleteMapping("/clear")
    public Result<Boolean> clear(Authentication auth) {
        return Result.ok(notificationService.clear(auth.getName()));
    }
}
