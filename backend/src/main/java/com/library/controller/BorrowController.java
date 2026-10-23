package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.borrow.BorrowCreateRequest;
import com.library.dto.borrow.OverdueRequest;
import com.library.dto.query.BorrowQuery;
import com.library.entity.BorrowRecord;
import com.library.service.BorrowService;
import com.library.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅接口
 */
@RestController
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final SysLogService sysLogService;

    /** 借阅记录分页查询 */
    @GetMapping("/page")
    public Result<PageResult<BorrowRecord>> page(BorrowQuery query) {
        return Result.ok(borrowService.page(query));
    }

    /** 办理借阅 */
    @PostMapping
    public Result<BorrowRecord> create(@Valid @RequestBody BorrowCreateRequest req,
                                       Authentication auth, HttpServletRequest request) {
        BorrowRecord record = borrowService.create(req);
        sysLogService.log(auth.getName(), "借阅管理", "办理借阅", clientIp(request), 1);
        return Result.ok(record);
    }

    /** 归还图书 */
    @PutMapping("/{id}/return")
    public Result<BorrowRecord> returnBook(@PathVariable Long id,
                                           Authentication auth, HttpServletRequest request) {
        BorrowRecord record = borrowService.returnBook(id);
        sysLogService.log(auth.getName(), "借阅管理", "归还图书", clientIp(request), 1);
        return Result.ok(record);
    }

    /** 续借 */
    @PutMapping("/{id}/renew")
    public Result<BorrowRecord> renew(@PathVariable Long id,
                                      Authentication auth, HttpServletRequest request) {
        BorrowRecord record = borrowService.renew(id);
        sysLogService.log(auth.getName(), "借阅管理", "续借图书", clientIp(request), 1);
        return Result.ok(record);
    }

    /** 逾期处理 */
    @PutMapping("/{id}/overdue")
    public Result<BorrowRecord> overdue(@PathVariable Long id, @RequestBody OverdueRequest req,
                                        Authentication auth, HttpServletRequest request) {
        BorrowRecord record = borrowService.overdue(id, req.getPenalty());
        sysLogService.log(auth.getName(), "借阅管理",
                "逾期处理(罚款" + req.getPenalty() + "元)", clientIp(request), 1);
        return Result.ok(record);
    }

    /** 删除历史借阅记录(仅已归还可删) */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id,
                                  Authentication auth, HttpServletRequest request) {
        boolean ok = borrowService.remove(id);
        sysLogService.log(auth.getName(), "借阅管理", "删除借阅记录", clientIp(request), 1);
        return Result.ok(ok);
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
