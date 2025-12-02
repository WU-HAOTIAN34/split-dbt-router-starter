package com.wht.sdt.strategy.impl;

import com.wht.sdt.context.SDTContext;
import com.wht.sdt.context.StrategyContext;
import com.wht.sdt.strategy.RouterStrategy;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 基于时间的路由策略实现
 * 根据时间戳进行分库分表，适合时序数据
 * 支持多种时间粒度：按天、按月、按年
 *
 * @author wht
 */
@Slf4j
public class RouterStrategyTimeBased implements RouterStrategy {

    @Override
    public void splitDB(StrategyContext strategyContext) {
        int size = strategyContext.getDbCount();

        // 解析时间值（支持时间戳或日期字符串）
        long timestamp = parseTimestamp(strategyContext.getKeyValue());
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault());

        // 使用年月作为分库依据
        int yearMonth = dateTime.getYear() * 100 + dateTime.getMonthValue();
        int dbIdx = yearMonth % size + 1;

        // 格式化数据库 key
        String dbKey = "db" + String.format("%02d", dbIdx);

        SDTContext.setDBKey(dbKey);

        log.debug("[Router-DB-Time] key={} timestamp={} yearMonth={} dbIdx={} dbKey={}",
                strategyContext.getKeyValue(), timestamp, yearMonth, dbIdx, dbKey);
    }

    @Override
    public void splitTB(StrategyContext strategyContext) {
        int size = strategyContext.getTbCount();

        // 解析时间值（支持时间戳或日期字符串）
        long timestamp = parseTimestamp(strategyContext.getKeyValue());
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault());

        // 使用日期作为分表依据（年月日）
        int dateNum = dateTime.getYear() * 10000 + dateTime.getMonthValue() * 100 + dateTime.getDayOfMonth();
        int tbIdx = dateNum % size + 1;

        // 格式化表 key
        String tbKey = String.format("%03d", tbIdx);

        SDTContext.setTBKey(tbKey);

        log.debug("[Router-TB-Time] key={} timestamp={} dateNum={} tbIdx={} tbKey={}",
                strategyContext.getKeyValue(), timestamp, dateNum, tbIdx, tbKey);
    }

    /**
     * 解析时间戳
     * 支持：时间戳（毫秒）、时间戳（秒）、日期字符串
     */
    private long parseTimestamp(String value) {
        try {
            // 尝试解析为长整型（时间戳）
            long timestamp = Long.parseLong(value);

            // 判断是秒还是毫秒（如果小于10000000000，认为是秒）
            if (timestamp < 10000000000L) {
                timestamp = timestamp * 1000;
            }

            return timestamp;
        } catch (NumberFormatException e) {
            // 尝试解析为日期字符串（支持多种格式）
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value + " 00:00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ex) {
                log.error("Failed to parse timestamp: {}", value, ex);
                // 返回当前时间戳作为降级
                return System.currentTimeMillis();
            }
        }
    }

    public void clear() {
        SDTContext.clearAll();
    }
}
