package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.BusinessException;
import com.library.common.PageResult;
import com.library.dto.query.ReaderQuery;
import com.library.entity.Reader;
import com.library.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 读者服务
 */
@Service
@RequiredArgsConstructor
public class ReaderService {

    private final ReaderMapper readerMapper;

    public PageResult<Reader> page(ReaderQuery query) {
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<Reader>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Reader::getName, query.getKeyword())
                        .or().like(Reader::getReaderNo, query.getKeyword())
                        .or().like(Reader::getPhone, query.getKeyword()))
                .eq(StringUtils.hasText(query.getReaderType()), Reader::getReaderType, query.getReaderType())
                .eq(query.getStatus() != null, Reader::getStatus, query.getStatus())
                .orderByDesc(Reader::getId);
        Page<Reader> page = readerMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public Reader detail(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BusinessException(404, "读者不存在");
        }
        return reader;
    }

    @Transactional
    public Reader create(Reader reader) {
        Long exists = readerMapper.selectCount(new LambdaQueryWrapper<Reader>()
                .eq(Reader::getReaderNo, reader.getReaderNo()));
        if (exists != null && exists > 0) {
            throw new BusinessException("读者证号已存在");
        }
        if (reader.getStatus() == null) {
            reader.setStatus(1);
        }
        if (reader.getBorrowCount() == null) {
            reader.setBorrowCount(0);
        }
        if (reader.getMaxBorrow() == null) {
            reader.setMaxBorrow(maxBorrowOf(reader.getReaderType()));
        }
        if (reader.getGender() == null) {
            reader.setGender(0);
        }
        reader.setCreateTime(LocalDateTime.now());
        readerMapper.insert(reader);
        return reader;
    }

    @Transactional
    public Reader update(Long id, Reader reader) {
        if (readerMapper.selectById(id) == null) {
            throw new BusinessException(404, "读者不存在");
        }
        reader.setId(id);
        readerMapper.updateById(reader);
        return readerMapper.selectById(id);
    }

    @Transactional
    public void remove(Long id) {
        if (readerMapper.selectById(id) == null) {
            throw new BusinessException(404, "读者不存在");
        }
        readerMapper.deleteById(id);
    }

    private int maxBorrowOf(String type) {
        return switch (type == null ? "" : type) {
            case "教师" -> 10;
            case "职工" -> 8;
            case "学生" -> 5;
            case "校外" -> 3;
            default -> 5;
        };
    }
}
