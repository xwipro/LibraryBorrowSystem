package com.library.controller;

import com.library.common.Result;
import com.library.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据看板接口
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 核心统计指标 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(dashboardService.stats());
    }

    /** 借阅趋势(近 N 天) */
    @GetMapping("/borrow-trend")
    public Result<List<Map<String, Object>>> borrowTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(dashboardService.borrowTrend(days));
    }

    /** 图书分类占比 */
    @GetMapping("/category-stats")
    public Result<List<Map<String, Object>>> categoryStats() {
        return Result.ok(dashboardService.categoryStats());
    }

    /** 热门图书 TOP */
    @GetMapping("/hot-books")
    public Result<List<Map<String, Object>>> hotBooks(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.hotBooks(limit));
    }
}
