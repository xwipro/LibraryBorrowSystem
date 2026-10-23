package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.borrow.BorrowCreateRequest;
import com.library.dto.query.BorrowQuery;
import com.library.entity.Book;
import com.library.entity.BorrowRecord;
import com.library.entity.Reader;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 借阅服务: 借阅 / 归还 / 续借 / 逾期
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderMapper readerMapper;
    private final BookMapper bookMapper;

    @Value("${app.borrow-days:30}")
    private int borrowDays;

    public PageResult<BorrowRecord> page(BorrowQuery query) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(query.getStatus() != null, BorrowRecord::getStatus, query.getStatus())
                .eq(query.getReaderId() != null, BorrowRecord::getReaderId, query.getReaderId())
                .orderByDesc(BorrowRecord::getBorrowTime);
        // 关键词: 按读者姓名 / 书名模糊过滤
        if (StringUtils.hasText(query.getKeyword())) {
            Collection<Long> readerIds = readerMapper.selectList(new LambdaQueryWrapper<Reader>()
                            .like(Reader::getName, query.getKeyword()))
                    .stream().map(Reader::getId).toList();
            Collection<Long> bookIds = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                            .like(Book::getTitle, query.getKeyword()))
                    .stream().map(Book::getId).toList();
            if (readerIds.isEmpty() && bookIds.isEmpty()) {
                return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
            }
            wrapper.and(w -> w.in(BorrowRecord::getReaderId, readerIds)
                    .or().in(BorrowRecord::getBookId, bookIds));
        }
        Page<BorrowRecord> page = borrowRecordMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        fillJoin(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 办理借阅 */
    @Transactional
    public BorrowRecord create(BorrowCreateRequest req) {
        Reader reader = readerMapper.selectById(req.getReaderId());
        if (reader == null) {
            throw new BusinessException(400, "读者不存在");
        }
        if (reader.getStatus() == null || reader.getStatus() != 1) {
            throw new BusinessException(400, "读者已被禁用");
        }
        if (reader.getBorrowCount() != null && reader.getMaxBorrow() != null
                && reader.getBorrowCount() >= reader.getMaxBorrow()) {
            throw new BusinessException(400, "该读者已达最大借阅上限");
        }
        Book book = bookMapper.selectById(req.getBookId());
        if (book == null) {
            throw new BusinessException(400, "图书不存在");
        }
        if (book.getStockCount() == null || book.getStockCount() <= 0) {
            throw new BusinessException(400, "该图书库存不足，无法借阅");
        }

        book.setStockCount(book.getStockCount() - 1);
        book.setBorrowCount((book.getBorrowCount() == null ? 0 : book.getBorrowCount()) + 1);
        bookMapper.updateById(book);
        reader.setBorrowCount(reader.getBorrowCount() == null ? 1 : reader.getBorrowCount() + 1);
        readerMapper.updateById(reader);

        BorrowRecord record = new BorrowRecord();
        record.setReaderId(reader.getId());
        record.setBookId(book.getId());
        record.setBorrowTime(LocalDateTime.now());
        record.setDueTime(LocalDateTime.now().plusDays(borrowDays));
        record.setStatus(0);
        record.setRenewCount(0);
        borrowRecordMapper.insert(record);
        fillJoin(List.of(record));
        return record;
    }

    /** 归还 */
    @Transactional
    public BorrowRecord returnBook(Long id) {
        BorrowRecord record = requireRecord(id);
        if (record.getStatus() == 1) {
            throw new BusinessException(400, "该记录已归还");
        }
        record.setStatus(1);
        record.setReturnTime(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setStockCount(book.getStockCount() == null ? 1 : book.getStockCount() + 1);
            bookMapper.updateById(book);
        }
        Reader reader = readerMapper.selectById(record.getReaderId());
        if (reader != null) {
            reader.setBorrowCount(Math.max(0, (reader.getBorrowCount() == null ? 0 : reader.getBorrowCount()) - 1));
            readerMapper.updateById(reader);
        }
        fillJoin(List.of(record));
        return record;
    }

    /** 续借: 仅借阅中可续, 最多 2 次, 应还时间 +30 天 */
    @Transactional
    public BorrowRecord renew(Long id) {
        BorrowRecord record = requireRecord(id);
        if (record.getStatus() != 0) {
            throw new BusinessException(400, "当前状态不可续借");
        }
        if (record.getRenewCount() >= 2) {
            throw new BusinessException(400, "已达续借次数上限");
        }
        record.setRenewCount(record.getRenewCount() + 1);
        record.setDueTime(record.getDueTime().plusDays(borrowDays));
        borrowRecordMapper.updateById(record);
        fillJoin(List.of(record));
        return record;
    }

    /** 逾期处理(罚款): 罚款并直接闭环为已归还, 恢复库存/在借数, 之后可由管理员删除或保留 */
    @Transactional
    public BorrowRecord overdue(Long id, int penalty) {
        BorrowRecord record = requireRecord(id);
        if (record.getStatus() == 1) {
            throw new BusinessException(400, "已归还记录不可做逾期处理");
        }
        // 罚款即视为归还: 记录归档为已归还并记录实还时间
        record.setStatus(1);
        record.setReturnTime(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        // 恢复图书库存与读者在借数(与正常归还一致)
        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setStockCount(book.getStockCount() == null ? 1 : book.getStockCount() + 1);
            bookMapper.updateById(book);
        }
        Reader reader = readerMapper.selectById(record.getReaderId());
        if (reader != null) {
            reader.setBorrowCount(Math.max(0, (reader.getBorrowCount() == null ? 0 : reader.getBorrowCount()) - 1));
            readerMapper.updateById(reader);
        }
        fillJoin(List.of(record));
        return record;
    }

    /** 删除历史借阅记录: 仅已归还(状态1)可删, 未闭环记录不可删(避免账目/库存错乱) */
    @Transactional
    public boolean remove(Long id) {
        BorrowRecord record = requireRecord(id);
        if (record.getStatus() == null || record.getStatus() != 1) {
            throw new BusinessException(400, "仅已归还的记录可删除");
        }
        return borrowRecordMapper.deleteById(id) > 0;
    }

    private BorrowRecord requireRecord(Long id) {
        BorrowRecord record = borrowRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        return record;
    }

    private void fillJoin(Collection<BorrowRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<Long, Reader> readers = readerMapper.selectList(null).stream()
                .collect(Collectors.toMap(Reader::getId, Function.identity()));
        Map<Long, Book> books = bookMapper.selectList(null).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
        records.forEach(r -> {
            Reader reader = readers.get(r.getReaderId());
            Book book = books.get(r.getBookId());
            r.setReaderName(reader != null ? reader.getName() : "未知读者");
            r.setBookTitle(book != null ? book.getTitle() : "未知图书");
            r.setIsbn(book != null ? book.getIsbn() : "");
        });
    }
}
