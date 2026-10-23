package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.query.ReservationQuery;
import com.library.entity.Book;
import com.library.entity.Reader;
import com.library.entity.Reservation;
import com.library.mapper.BookMapper;
import com.library.mapper.ReaderMapper;
import com.library.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预约服务
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final ReaderMapper readerMapper;
    private final BookMapper bookMapper;

    public PageResult<Reservation> page(ReservationQuery query) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(query.getStatus() != null, Reservation::getStatus, query.getStatus())
                .orderByDesc(Reservation::getReserveTime);
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
            wrapper.and(w -> w.in(Reservation::getReaderId, readerIds)
                    .or().in(Reservation::getBookId, bookIds));
        }
        Page<Reservation> page = reservationMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        fillJoin(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 审批预约: 仅待处理可审批, 审批后置为已通知 */
    @Transactional
    public Reservation approve(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (reservation.getStatus() != 0) {
            throw new BusinessException(400, "仅待处理的预约可审批");
        }
        reservation.setStatus(1);
        reservationMapper.updateById(reservation);
        fillJoin(List.of(reservation));
        return reservation;
    }

    /** 删除历史预约记录: 仅已通知(1)/已取消(2)可删, 待处理(0)不可删(未闭环) */
    @Transactional
    public boolean remove(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (reservation.getStatus() == null || reservation.getStatus() == 0) {
            throw new BusinessException(400, "待处理的预约不可删除");
        }
        return reservationMapper.deleteById(id) > 0;
    }

    private void fillJoin(Collection<Reservation> records) {
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
        });
    }
}
