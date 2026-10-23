package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.announcement.AnnouncementCreateRequest;
import com.library.dto.query.AnnouncementQuery;
import com.library.entity.Announcement;
import com.library.service.AnnouncementService;
import com.library.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告接口: 管理端发布/管理, 读者端拉取
 */
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final SysLogService sysLogService;

    /** 管理端: 发布公告 */
    @PostMapping
    public Result<Announcement> create(@Valid @RequestBody AnnouncementCreateRequest request,
                                       Authentication auth, HttpServletRequest servletRequest) {
        Announcement created = announcementService.create(request.getTitle(), request.getContent(),
                auth.getName());
        sysLogService.log(auth.getName(), "系统设置", "发布公告", clientIp(servletRequest), 1);
        return Result.ok(created);
    }

    /** 管理端: 公告分页 */
    @GetMapping("/page")
    public Result<PageResult<Announcement>> page(AnnouncementQuery query) {
        return Result.ok(announcementService.page(query));
    }

    /** 管理端: 删除公告 */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id,
                                  Authentication auth, HttpServletRequest servletRequest) {
        announcementService.remove(id);
        sysLogService.log(auth.getName(), "系统设置", "删除公告", clientIp(servletRequest), 1);
        return Result.ok(true);
    }

    /** 管理端: 置顶公告(可多条置顶) */
    @PutMapping("/{id}/pin")
    public Result<Announcement> pin(@PathVariable Long id,
                                    Authentication auth, HttpServletRequest servletRequest) {
        Announcement updated = announcementService.setPinned(id, true);
        sysLogService.log(auth.getName(), "系统设置", "置顶公告", clientIp(servletRequest), 1);
        return Result.ok(updated);
    }

    /** 管理端: 取消置顶 */
    @PutMapping("/{id}/unpin")
    public Result<Announcement> unpin(@PathVariable Long id,
                                      Authentication auth, HttpServletRequest servletRequest) {
        Announcement updated = announcementService.setPinned(id, false);
        sysLogService.log(auth.getName(), "系统设置", "取消置顶", clientIp(servletRequest), 1);
        return Result.ok(updated);
    }

    /** 客户端(读者端): 已发布公告列表 */
    @GetMapping("/client/list")
    public Result<List<Announcement>> clientList(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(announcementService.clientList(limit));
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
