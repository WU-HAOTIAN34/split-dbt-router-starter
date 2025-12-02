package com.wht.sdt.annotation;




import com.wht.sdt.enumeration.StrategyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分库分表策略注解
 * 用于标记DAO类，表示该类需要进行分表操作
 *
 * @author wht
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SplitDT {

    /**
     * 分库分表字段
     *
     * @return 分库分表字段
     */
    int groupKey() default 1;

    /**
     * 路由字段
     *
     * @return 路由字段，如果为空则使用配置文件中的默认字段
     */
    String routeKey() default "";

    /**
     * 分库分表字段
     *
     * @return 路由字段，如果为空则使用配置文件中的默认字段
     */
    StrategyType strategy() default StrategyType.HASH;
}
