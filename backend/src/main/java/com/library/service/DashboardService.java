package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.entity.Book;
import com.library.entity.BookCategory;
import com.library.entity.BorrowRecord;
import com.library.entity.Reader;
import com.library.mapper.BookCategoryMapper;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookCategoryMapper categoryMapper;

    /** 核心统计 */
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", bookMapper.selectCount(null));
        stats.put("totalReaders", readerMapper.selectCount(null));
        Long overdue = borrowRecordMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getStatus, 2));
        stats.put("overdueBooks", overdue == null ? 0 : overdue);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long today = borrowRecordMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .ge(BorrowRecord::getBorrowTime, startOfDay));
        stats.put("todayBorrows", today == null ? 0 : today);
        return stats;
    }

    /** 近 N 天借阅趋势(缺日补 0) */
    public List<Map<String, Object>> borrowTrend(int days) {
        int safe = Math.min(Math.max(days, 1), 90);
        Map<String, Long> countByDate = new HashMap<>();
        borrowRecordMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                        .ge(BorrowRecord::getBorrowTime, LocalDate.now().minusDays(safe - 1L).atStartOfDay()))
                .forEach(r -> {
                    String date = r.getBorrowTime().toLocalDate().toString();
                    countByDate.merge(date, 1L, Long::sum);
                });
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = safe - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("count", countByDate.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    /** 图书分类占比(馆藏量) */
    public List<Map<String, Object>> categoryStats() {
        Map<Long, Long> countByCategory = new HashMap<>();
        bookMapper.selectList(null).forEach(b ->
                countByCategory.merge(b.getCategoryId(), b.getTotalCount() == null ? 0L : b.getTotalCount().longValue(), Long::sum));
        List<Map<String, Object>> result = new ArrayList<>();
        categoryMapper.selectList(new LambdaQueryWrapper<BookCategory>().orderByAsc(BookCategory::getSortOrder))
                .forEach(c -> {
                    Long value = countByCategory.getOrDefault(c.getId(), 0L);
                    if (value > 0) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", c.getName());
                        item.put("value", value);
                        result.add(item);
                    }
                });
        return result;
    }

    /** 热门图书 TOP */
    public List<Map<String, Object>> hotBooks(int limit) {
        int safe = Math.min(Math.max(limit, 1), 50);
        return bookMapper.selectList(new LambdaQueryWrapper<Book>()
                        .orderByDesc(Book::getBorrowCount)
                        .last("LIMIT " + safe))
                .stream().map(b -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("title", b.getTitle());
                    item.put("count", b.getBorrowCount() == null ? 0 : b.getBorrowCount());
                    return item;
                }).toList();
    }
}
