package com.wht.sdt.strategy.impl;

import com.wht.sdt.context.SDTContext;
import com.wht.sdt.context.StrategyContext;
import com.wht.sdt.strategy.RouterStrategy;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 基于一致性哈希的路由策略实现
 * 使用一致性哈希算法进行分库分表，支持虚拟节点
 * 优点：数据分布均匀，扩容时数据迁移量小
 *
 * @author wht
 */
@Slf4j
public class RouterStrategyConsistentHash implements RouterStrategy {

    // 虚拟节点数量
    private static final int VIRTUAL_NODE_COUNT = 150;

    // 哈希环（数据库）
    private final SortedMap<Long, Integer> dbHashRing = new TreeMap<>();

    // 哈希环（表）
    private final SortedMap<Long, Integer> tbHashRing = new TreeMap<>();

    @Override
    public void splitDB(StrategyContext strategyContext) {
        int size = strategyContext.getDbCount();

        // 初始化哈希环（如果为空）
        if (dbHashRing.isEmpty()) {
            initHashRing(dbHashRing, size);
        }

        // 计算键的哈希值
        long hash = hash(strategyContext.getKeyValue());

        // 在哈希环上找到对应的节点
        int dbIdx = getNodeIndex(dbHashRing, hash);

        // 格式化数据库 key
        String dbKey = "db" + String.format("%02d", dbIdx);

        SDTContext.setDBKey(dbKey);

        log.debug("[Router-DB-ConsistentHash] key={} hash={} dbIdx={} dbKey={}",
                strategyContext.getKeyValue(), hash, dbIdx, dbKey);
    }

    @Override
    public void splitTB(StrategyContext strategyContext) {
        int size = strategyContext.getTbCount();

        // 初始化哈希环（如果为空）
        if (tbHashRing.isEmpty()) {
            initHashRing(tbHashRing, size);
        }

        // 计算键的哈希值
        long hash = hash(strategyContext.getKeyValue());

        // 在哈希环上找到对应的节点
        int tbIdx = getNodeIndex(tbHashRing, hash);

        // 格式化表 key
        String tbKey = String.format("%03d", tbIdx);

        SDTContext.setTBKey(tbKey);

        log.debug("[Router-TB-ConsistentHash] key={} hash={} tbIdx={} tbKey={}",
                strategyContext.getKeyValue(), hash, tbIdx, tbKey);
    }

    /**
     * 初始化哈希环，添加虚拟节点
     */
    private void initHashRing(SortedMap<Long, Integer> hashRing, int nodeCount) {
        for (int i = 1; i <= nodeCount; i++) {
            // 为每个物理节点创建多个虚拟节点
            for (int j = 0; j < VIRTUAL_NODE_COUNT; j++) {
                String virtualNodeKey = i + "-VN" + j;
                long hash = hash(virtualNodeKey);
                hashRing.put(hash, i);
            }
        }
        log.debug("Initialized hash ring with {} physical nodes and {} virtual nodes per node",
                nodeCount, VIRTUAL_NODE_COUNT);
    }

    /**
     * 在哈希环上找到对应的节点索引
     */
    private int getNodeIndex(SortedMap<Long, Integer> hashRing, long hash) {
        if (hashRing.isEmpty()) {
            return 1;
        }

        // 找到第一个大于等于hash的节点
        SortedMap<Long, Integer> tailMap = hashRing.tailMap(hash);

        // 如果没有找到，返回环上的第一个节点（环形结构）
        if (tailMap.isEmpty()) {
            return hashRing.get(hashRing.firstKey());
        }

        return tailMap.get(tailMap.firstKey());
    }

    /**
     * 使用MD5计算哈希值
     */
    private long hash(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(key.getBytes());

            // 取前8个字节转换为long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            return Math.abs(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found", e);
            // 降级使用hashCode
            return Math.abs(key.hashCode());
        }
    }

    public void clear() {
        SDTContext.clearAll();
    }
}
