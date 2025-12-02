package com.wht.sdt.strategy.impl;

import com.wht.sdt.context.SDTContext;
import com.wht.sdt.context.StrategyContext;
import com.wht.sdt.strategy.RouterStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于哈希值的路由策略实现
 * 使用字段值的哈希码进行分库分表计算
 *
 * @author wht
 */
@Slf4j
public class RouterStrategyHashCode implements RouterStrategy {
    ;

    @Override
    public void splitDB(StrategyContext strategyContext) {
        int dbCount = strategyContext.getDbCount();

        // 1. 计算哈希值（使用HashMap的扰动函数，提高散列均匀性）
        int hash = strategyContext.getKeyValue().hashCode();
        hash = hash ^ (hash >>> 16);

        // 2. 分库：使用哈希值对库数取模
        // 使用绝对值避免负数，+1 是因为库编号从 1 开始（db01, db02...）
        int dbIdx = (Math.abs(hash) % dbCount) + 1;

        // 3. 格式化数据库 key
        String dbKey = "db" + String.format("%02d", dbIdx);

        SDTContext.setDBKey(dbKey);

        log.debug("[Router-DB] key={} hash={} dbIdx={} dbKey={}",
                strategyContext.getKeyValue(), hash, dbIdx, dbKey);
    }

    @Override
    public void splitTB(StrategyContext strategyContext) {
        int tbCount = strategyContext.getTbCount();

        // 1. 计算哈希值（使用HashMap的扰动函数，提高散列均匀性）
        int hash = strategyContext.getKeyValue().hashCode();
        hash = hash ^ (hash >>> 16);

        // 2. 分表：使用哈希值除以库数后再取模
        // 这样可以确保数据在库表中均匀分布，避免单数库只存单数表的问题
        // 除以库数是为了让分库和分表使用不同的哈希段，提高分布均匀性
        int tbIdx = (Math.abs(hash / strategyContext.getDbCount()) % tbCount) + 1;

        // 3. 格式化表 key
        String tbKey = String.format("%03d", tbIdx);

        SDTContext.setTBKey(tbKey);

        log.debug("[Router-TB] key={} hash={} tbIdx={} tbKey={}",
                strategyContext.getKeyValue(), hash, tbIdx, tbKey);
    }

    public void clear() {
        SDTContext.clearAll();
    }

}
