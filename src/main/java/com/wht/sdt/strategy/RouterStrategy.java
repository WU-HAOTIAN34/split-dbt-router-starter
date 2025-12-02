package com.wht.sdt.strategy;

import com.wht.sdt.context.StrategyContext;

/**
 * 数据库路由策略接口
 * 定义路由策略的核心方法
 *
 * @author wht
 */

public interface RouterStrategy {


    /**
     * 分库
     *
     */
    public void splitDB(StrategyContext strategyContext);

    /**
     * 分表
     *
     */
    public void splitTB(StrategyContext strategyContext);

    /**
     * 清除路由信息
     */
    public void clear();
}
