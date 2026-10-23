package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.query.ReservationQuery;
import com.library.entity.Reservation;
import com.library.service.ReservationService;
import com.library.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 预约接口
 */
@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final SysLogService sysLogService;

    /** 预约记录分页查询 */
    @GetMapping("/page")
    public Result<PageResult<Reservation>> page(ReservationQuery query) {
        return Result.ok(reservationService.page(query));
    }

    /** 审批预约 */
    @PutMapping("/{id}/approve")
    public Result<Reservation> approve(@PathVariable Long id,
                                       Authentication auth, HttpServletRequest request) {
        Reservation reservation = reservationService.approve(id);
        sysLogService.log(auth.getName(), "借阅管理", "审批预约", clientIp(request), 1);
        return Result.ok(reservation);
    }

    /** 删除历史预约记录(仅已通知/已取消可删) */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id,
                                  Authentication auth, HttpServletRequest request) {
        boolean ok = reservationService.remove(id);
        sysLogService.log(auth.getName(), "借阅管理", "删除预约记录", clientIp(request), 1);
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
