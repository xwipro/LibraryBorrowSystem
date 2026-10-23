package com.library.controller;

import com.library.common.PageResult;
import com.library.common.Result;
import com.library.dto.query.BookQuery;
import com.library.entity.Book;
import com.library.entity.BookCategory;
import com.library.service.BookService;
import com.library.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书接口
 */
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final SysLogService sysLogService;

    /** 图书分页查询 */
    @GetMapping("/page")
    public Result<PageResult<Book>> page(BookQuery query) {
        return Result.ok(bookService.page(query));
    }

    /** 图书详情 */
    @GetMapping("/{id}")
    public Result<Book> detail(@PathVariable Long id) {
        return Result.ok(bookService.detail(id));
    }

    /** 新增图书 */
    @PostMapping
    public Result<Book> create(@RequestBody Book book, Authentication auth, HttpServletRequest request) {
        Book created = bookService.create(book);
        sysLogService.log(auth.getName(), "图书管理", "新增图书", clientIp(request), 1);
        return Result.ok(created);
    }

    /** 更新图书 */
    @PutMapping("/{id}")
    public Result<Book> update(@PathVariable Long id, @RequestBody Book book,
                               Authentication auth, HttpServletRequest request) {
        Book updated = bookService.update(id, book);
        sysLogService.log(auth.getName(), "图书管理", "修改图书", clientIp(request), 1);
        return Result.ok(updated);
    }

    /** 删除图书 */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        bookService.remove(id);
        sysLogService.log(auth.getName(), "图书管理", "删除图书", clientIp(request), 1);
        return Result.ok(true);
    }

    /** 图书分类列表 */
    @GetMapping("/categories")
    public Result<List<BookCategory>> categories() {
        return Result.ok(bookService.categories());
    }

    /** 推荐图书 */
    @GetMapping("/recommend")
    public Result<List<Book>> recommend(@RequestParam Long readerId) {
        return Result.ok(bookService.recommend(readerId));
    }

    /** 热门借阅 TOP */
    @GetMapping("/hot")
    public Result<List<Book>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(bookService.hot(limit));
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
