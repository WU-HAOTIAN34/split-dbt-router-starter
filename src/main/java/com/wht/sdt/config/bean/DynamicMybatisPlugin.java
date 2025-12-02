package com.wht.sdt.config.bean;

import com.wht.sdt.context.SDTContext;
import com.wht.sdt.context.StrategyContext;
import com.wht.sdt.enumeration.StrategyType;
import com.wht.sdt.properties.DataSourceGroup;
import com.wht.sdt.properties.RouterGroupProperties;
import com.wht.sdt.strategy.RouterStrategy;
import com.wht.sdt.util.SqlTableReplacer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Map;

/**
 * MyBatis动态表名插件（优化版）
 * 使用JSqlParser进行SQL解析，支持复杂SQL（JOIN、子查询等）
 * 相比正则表达式方式，性能更好，准确性更高
 *
 * @author wht
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class })
})
@Slf4j
public class DynamicMybatisPlugin implements Interceptor {

    private final RouterGroupProperties routerGroupProperties;
    private final Map<StrategyType, RouterStrategy> routerStrategies;

    public DynamicMybatisPlugin(RouterGroupProperties routerGroupProperties,
            Map<StrategyType, RouterStrategy> routerStrategies) {
        this.routerGroupProperties = routerGroupProperties;
        this.routerStrategies = routerStrategies;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取StatementHandler
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 获取BoundSql
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // 检查是否需要进行分表路由
        String tbKey = SDTContext.getTBKey();
        if (tbKey == null || tbKey.isEmpty()) {
            // 不需要分表，直接执行
            return invocation.proceed();
        }

        String groupKey = SDTContext.getGKey();
        if (groupKey == null) {
            log.warn("GroupKey is null, skip table routing");
            return invocation.proceed();
        }

        // 获取数据源组配置
        DataSourceGroup dataSourceGroup = routerGroupProperties.getGroups().get(groupKey);
        if (dataSourceGroup == null) {
            log.warn("DataSourceGroup not found for groupKey: {}", groupKey);
            return invocation.proceed();
        }

        // 获取分表配置
        Map<String, Integer> tbCounts = dataSourceGroup.getTbCounts();
        if (tbCounts == null || tbCounts.isEmpty()) {
            log.debug("No table sharding config found for group: {}", groupKey);
            return invocation.proceed();
        }

        // 执行分表路由策略
        try {
            StrategyContext strategyContext = StrategyContext.builder()
                    .tbCount(getMaxTbCount(tbCounts))
                    .dbCount(dataSourceGroup.getDbCount())
                    .keyValue(tbKey)
                    .build();

            // 执行路由策略，计算表后缀
            StrategyType strategyType = SDTContext.getStrategyType();
            if (strategyType == null) {
                strategyType = StrategyType.HASH;
            }

            RouterStrategy strategy = routerStrategies.get(strategyType);
            if (strategy != null) {
                strategy.splitTB(strategyContext);
            } else {
                log.warn("RouterStrategy not found for type: {}", strategyType);
            }

        } catch (Exception e) {
            log.error("Error executing table routing strategy", e);
        }

        // 获取计算后的表后缀
        String tableSuffix = SDTContext.getTBKey();

        // 使用SqlTableReplacer替换SQL中的表名
        String modifiedSql = SqlTableReplacer.replaceTableName(originalSql, tbCounts, tableSuffix);

        // 只有当SQL被修改时才更新
        if (!originalSql.equals(modifiedSql)) {
            metaObject.setValue("delegate.boundSql.sql", modifiedSql);

            log.debug("[SQL-Rewrite] Original: {}", originalSql);
            log.debug("[SQL-Rewrite] Modified: {}", modifiedSql);
            log.debug("[SQL-Rewrite] GroupKey: {}, TableSuffix: {}", groupKey, tableSuffix);
        }

        // 执行修改后的SQL
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        // 只拦截StatementHandler
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    /**
     * 获取最大分表数量（用于路由计算）
     */
    private int getMaxTbCount(Map<String, Integer> tbCounts) {
        if (tbCounts == null || tbCounts.isEmpty()) {
            return 1;
        }
        return tbCounts.values().stream()
                .max(Integer::compareTo)
                .orElse(1);
    }
}
