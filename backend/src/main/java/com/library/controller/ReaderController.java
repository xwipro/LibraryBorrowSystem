package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.query.ReaderQuery;
import com.library.entity.Reader;
import com.library.service.ReaderService;
import com.library.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 读者接口
 */
@RestController
@RequestMapping("/reader")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;
    private final SysLogService sysLogService;

    /** 读者分页查询 */
    @GetMapping("/page")
    public Result<PageResult<Reader>> page(ReaderQuery query) {
        return Result.ok(readerService.page(query));
    }

    /** 读者详情 */
    @GetMapping("/{id}")
    public Result<Reader> detail(@PathVariable Long id) {
        return Result.ok(readerService.detail(id));
    }

    /** 新增读者 */
    @PostMapping
    public Result<Reader> create(@RequestBody Reader reader, Authentication auth, HttpServletRequest request) {
        Reader created = readerService.create(reader);
        sysLogService.log(auth.getName(), "读者管理", "新增读者", clientIp(request), 1);
        return Result.ok(created);
    }

    /** 更新读者 */
    @PutMapping("/{id}")
    public Result<Reader> update(@PathVariable Long id, @RequestBody Reader reader,
                                 Authentication auth, HttpServletRequest request) {
        Reader updated = readerService.update(id, reader);
        sysLogService.log(auth.getName(), "读者管理", "修改读者", clientIp(request), 1);
        return Result.ok(updated);
    }

    /** 删除读者 */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        readerService.remove(id);
        sysLogService.log(auth.getName(), "读者管理", "删除读者", clientIp(request), 1);
        return Result.ok(true);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
