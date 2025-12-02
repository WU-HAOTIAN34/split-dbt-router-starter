package com.wht.sdt.aspect;

import com.wht.sdt.annotation.SplitDT;

import com.wht.sdt.config.DataSourceAutoConfig;
import com.wht.sdt.context.SDTContext;

import com.wht.sdt.context.StrategyContext;
import com.wht.sdt.enumeration.StrategyType;
import com.wht.sdt.properties.DataSourceGroup;
import com.wht.sdt.properties.RouterGroupProperties;

import com.wht.sdt.strategy.RouterStrategy;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 数据库路由切面
 * 拦截带有@DBRouter注解的方法，执行路由策略
 * 由 {@link DataSourceAutoConfig} 自动配置
 *
 * @author wht
 */
@Aspect
@Slf4j
public class SplitDTAspect {

    private final RouterGroupProperties routerGroupProperties;
    private final Map<StrategyType, RouterStrategy> routerStrategies;

    public SplitDTAspect(RouterGroupProperties routerGroupProperties,
                         Map<StrategyType, RouterStrategy> routerStrategies) {
        this.routerGroupProperties = routerGroupProperties;
        this.routerStrategies = routerStrategies;
    }

    /**
     * 定义切点：所有带有@SplitDT注解的方法
     */
    @Pointcut("@annotation(com.wht.sdt.annotation.SplitDT)")
    public void aopPoint() {
    }

    /**
     * 环绕通知：执行路由逻辑
     *
     * @param jp      切点
     * @param splitDT 路由注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("aopPoint() && @annotation(splitDT)")
    public Object doRouter(ProceedingJoinPoint jp, SplitDT splitDT) throws Throwable {

        // 获取路由字段
        String groupKey = "group" + String.format("%02d", splitDT.groupKey());
        StrategyType strategy = splitDT.strategy();
        String routeKey = splitDT.routeKey();

        SDTContext.setGKey(groupKey);

        if (strategy == null) {
            strategy = StrategyType.HASH;
        }
        SDTContext.setStrategyType(strategy);

        // 获取路由字段的值
        DataSourceGroup dataSourceGroup = routerGroupProperties.getGroups().get(groupKey);
        if (dataSourceGroup == null) {
            throw new RuntimeException("未找到数据源组配置: " + groupKey);
        }

        if (dataSourceGroup.getEnableSplit()) {

            if (routeKey == null || "".equals(routeKey)) {
                routeKey = dataSourceGroup.getRouterKey();
            }

            String dbKeyAttr = getAttrValue(routeKey, jp.getArgs());
            if (dbKeyAttr == null || dbKeyAttr.isEmpty()) {
                throw new RuntimeException("数据库路由key属性值为空: " + dataSourceGroup.getRouterKey());
            }

            SDTContext.setTBKey(dbKeyAttr);

            StrategyContext build = StrategyContext.builder()
                    .keyValue(SDTContext.getTBKey())
                    .dbCount(routerGroupProperties.getGroups().get(SDTContext.getGKey()).getDbCount())
                    .build();

            routerStrategies.get(SDTContext.getStrategyType()).splitDB(build);

        } else {
            SDTContext.setDBKey("db01");
            SDTContext.setTBKey(null);
        }

        try {
            // 执行目标方法
            return jp.proceed();
        } finally {
            // 清除路由信息
            SDTContext.clearAll();
        }
    }

    /**
     * 从方法参数中获取路由字段的值
     *
     * @param attr 路由字段名
     * @param args 方法参数
     * @return 路由字段值
     */
    private String getAttrValue(String attr, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        String filedValue = null;
        for (Object arg : args) {
            try {
                if (arg == null || arg instanceof String) {
                    continue;
                }

                Field declaredField = arg.getClass().getDeclaredField(attr);
                declaredField.setAccessible(true);
                Object fieldValue = declaredField.get(arg);
                if (fieldValue != null) {
                    filedValue = fieldValue.toString();
                }

                if (filedValue != null && !filedValue.isEmpty()) {
                    break;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return filedValue;
    }
}
