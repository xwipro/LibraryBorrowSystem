package com.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.PageResult;
import com.library.dto.query.LogQuery;
import com.library.entity.SysLog;
import com.library.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 操作日志服务: 查询 + 记录
 */
@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysLogMapper sysLogMapper;

    public PageResult<SysLog> page(LogQuery query) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(SysLog::getUsername, query.getKeyword())
                        .or().like(SysLog::getAction, query.getKeyword())
                        .or().like(SysLog::getIp, query.getKeyword()))
                .eq(StringUtils.hasText(query.getModule()), SysLog::getModule, query.getModule())
                .orderByDesc(SysLog::getTime);
        Page<SysLog> page = sysLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 写入一条操作日志(失败不阻断主流程) */
    public void log(String username, String module, String action, String ip, int status) {
        try {
            SysLog log = new SysLog();
            log.setUsername(username);
            log.setModule(module);
            log.setAction(action);
            log.setIp(ip);
            log.setTime(LocalDateTime.now());
            log.setStatus(status);
            sysLogMapper.insert(log);
        } catch (Exception e) {
            // 忽略日志写入失败
        }
    }
}
