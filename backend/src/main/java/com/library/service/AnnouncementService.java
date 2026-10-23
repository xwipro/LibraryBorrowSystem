package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.query.AnnouncementQuery;
import com.library.entity.Announcement;
import com.library.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务: 发布 / 分页 / 删除 / 客户端列表
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    /** 管理端分页(全部公告, 置顶优先 + 发布时间正序: 最先发布的在最上边) */
    public PageResult<Announcement> page(AnnouncementQuery query) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getCreateTime);
        Page<Announcement> page = announcementMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 发布公告: 发布即生效 */
    public Announcement create(String title, String content, String publisherName) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setPublisherName(publisherName);
        announcement.setStatus(1);
        announcement.setIsPinned(false);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return announcement;
    }

    /** 置顶/取消置顶(可多条置顶) */
    public Announcement setPinned(Long id, boolean pinned) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "公告不存在");
        }
        existing.setIsPinned(pinned);
        existing.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(existing);
        return existing;
    }

    /** 删除公告 */
    public void remove(Long id) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    /** 客户端(读者端)公告列表: 仅已发布, 置顶优先 + 发布时间正序(最先发布的在最上边), 取最近 limit 条 */
    public List<Announcement> clientList(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getCreateTime)
                .last("LIMIT " + safeLimit));
    }
}
