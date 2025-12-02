package com.wht.sdt.context;

import com.wht.sdt.enumeration.StrategyType;

/**
 * 数据库路由上下文持有者
 * 使用ThreadLocal保存当前线程的数据库路由信息
 *
 * @author wht
 */
public class SDTContext {

    /**
     * 存储数据库键（用于分库）
     */
    private static final ThreadLocal<String> dbKey = new ThreadLocal<>();

    /**
     * 存储表键值（用于分表）/ 表键
     */
    private static final ThreadLocal<String> tbKey = new ThreadLocal<>();
    /**
     * 组（用于数据源组）
     */
    private static final ThreadLocal<String> gKey = new ThreadLocal<>();
    /**
     * 策略
     */
    private static final ThreadLocal<StrategyType> strategy = new ThreadLocal<>();

    /**
     * 设置数据库键
     *
     * @param dbKeyValue 数据库键
     */
    public static void setDBKey(String dbKeyValue) {
        dbKey.set(dbKeyValue);
    }

    /**
     * 获取数据库键
     *
     * @return 数据库键
     */
    public static String getDBKey() {
        return dbKey.get();
    }

    /**
     * 设置表键
     *
     * @param tbKeyValue 表键
     */
    public static void setTBKey(String tbKeyValue) {
        tbKey.set(tbKeyValue);
    }

    /**
     * 获取表键
     *
     * @return 表键
     */
    public static String getTBKey() {
        return tbKey.get();
    }

    /**
     * 组键
     *
     * @param tbKeyValue 表键
     */
    public static void setGKey(String tbKeyValue) {
        gKey.set(tbKeyValue);
    }

    public static void setStrategyType(StrategyType strategyType) {
        strategy.set(strategyType);
    }

    public static StrategyType getStrategyType() {
        return strategy.get();
    }

    /**
     * 组键
     *
     * @return 表键
     */
    public static String getGKey() {
        return gKey.get();
    }

    /**
     * 清除所有路由信息
     */
    public static void clearAll() {
        dbKey.remove();
        tbKey.remove();
        gKey.remove();
        strategy.remove();
    }
}
