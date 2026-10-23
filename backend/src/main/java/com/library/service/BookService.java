package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.query.BookQuery;
import com.library.entity.Book;
import com.library.entity.BookCategory;
import com.library.mapper.BookCategoryMapper;
import com.library.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 图书服务
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;

    public PageResult<Book> page(BookQuery query) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Book::getTitle, query.getKeyword())
                        .or().like(Book::getAuthor, query.getKeyword())
                        .or().like(Book::getIsbn, query.getKeyword())
                        .or().like(Book::getPublisher, query.getKeyword()))
                .eq(query.getCategoryId() != null, Book::getCategoryId, query.getCategoryId())
                .eq(query.getStatus() != null, Book::getStatus, query.getStatus())
                .orderByDesc(Book::getId);
        Page<Book> page = bookMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        fillCategory(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Book detail(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        fillCategory(List.of(book));
        return book;
    }

    @Transactional
    public Book create(Book book) {
        Long exists = bookMapper.selectCount(new LambdaQueryWrapper<Book>()
                .eq(Book::getIsbn, book.getIsbn()));
        if (exists != null && exists > 0) {
            throw new BusinessException("ISBN 已存在");
        }
        if (book.getStockCount() == null) {
            book.setStockCount(0);
        }
        if (book.getTotalCount() == null) {
            book.setTotalCount(book.getStockCount());
        }
        if (book.getStatus() == null) {
            book.setStatus(1);
        }
        if (book.getBorrowCount() == null) {
            book.setBorrowCount(0);
        }
        if (book.getRating() == null) {
            book.setRating(java.math.BigDecimal.ZERO);
        }
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.insert(book);
        fillCategory(List.of(book));
        return book;
    }

    @Transactional
    public Book update(Long id, Book book) {
        Book current = bookMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(404, "图书不存在");
        }
        book.setId(id);
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
        Book updated = bookMapper.selectById(id);
        fillCategory(List.of(updated));
        return updated;
    }

    @Transactional
    public void remove(Long id) {
        if (bookMapper.selectById(id) == null) {
            throw new BusinessException(404, "图书不存在");
        }
        bookMapper.deleteById(id);
    }

    public List<BookCategory> categories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<BookCategory>().orderByAsc(BookCategory::getSortOrder));
    }

    /** 推荐图书(协同过滤占位: 返回可借的热门图书前 6 本) */
    public List<Book> recommend(Long readerId) {
        return bookMapper.selectList(new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .gt(Book::getStockCount, 0)
                        .orderByDesc(Book::getBorrowCount)
                        .last("LIMIT 6"))
                .stream().peek(b -> fillCategory(List.of(b))).toList();
    }

    /** 热门借阅 TOP */
    public List<Book> hot(int limit) {
        int safe = Math.min(Math.max(limit, 1), 50);
        return bookMapper.selectList(new LambdaQueryWrapper<Book>()
                        .orderByDesc(Book::getBorrowCount)
                        .last("LIMIT " + safe))
                .stream().peek(b -> fillCategory(List.of(b))).toList();
    }

    private void fillCategory(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName));
        books.forEach(b -> b.setCategoryName(nameMap.get(b.getCategoryId())));
    }
}
