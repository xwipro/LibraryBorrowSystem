package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.query.NotificationQuery;
import com.library.entity.Notification;
import com.library.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 通知服务: 列表 / 已读 / 删除 / 清空 / 未读数, 全部按当前登录用户名隔离
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public PageResult<Notification> page(NotificationQuery query, String username) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUsername, username)
                .eq(StringUtils.hasText(query.getType()), Notification::getType, query.getType())
                .orderByAsc(Notification::getCreateTime);
        Page<Notification> page = notificationMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 未读数 */
    public long unreadCount(String username) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUsername, username)
                .eq(Notification::getIsRead, false));
    }

    /** 标记单条已读(仅本人, 不可逆) */
    public boolean markRead(Long id, String username) {
        Notification n = requireOwned(id, username);
        if (Boolean.TRUE.equals(n.getIsRead())) {
            return true;
        }
        n.setIsRead(true);
        return notificationMapper.updateById(n) > 0;
    }

    /** 全部已读(仅本人, 不可逆) */
    public boolean markAllRead(String username) {
        return notificationMapper.update(null,
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUsername, username)
                        .set(Notification::getIsRead, true)) > 0;
    }

    /** 删除单条(仅本人) */
    public boolean remove(Long id, String username) {
        requireOwned(id, username);
        return notificationMapper.deleteById(id) > 0;
    }

    /** 清空本人全部通知 */
    public boolean clear(String username) {
        return notificationMapper.delete(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUsername, username)) > 0;
    }

    private Notification requireOwned(Long id, String username) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(404, "通知不存在");
        }
        if (!n.getUsername().equals(username)) {
            throw new BusinessException(403, "无权操作该通知");
        }
        return n;
    }
}
