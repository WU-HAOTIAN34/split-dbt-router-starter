package com.wht.sdt.config;


import com.wht.sdt.aspect.SplitDTAspect;
import com.wht.sdt.config.bean.DynamicDataSource;
import com.wht.sdt.config.bean.DynamicMybatisPlugin;
import com.wht.sdt.enumeration.StrategyType;
import com.wht.sdt.properties.DataSourceConfig;
import com.wht.sdt.properties.DataSourceGroup;
import com.wht.sdt.properties.RouterGroupProperties;
import com.wht.sdt.strategy.RouterStrategy;
import com.wht.sdt.strategy.RouterStrategyFactory;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库路由自动配置类
 * Spring Boot自动配置，初始化数据源、路由策略等Bean
 *
 * @author wht
 */
@Configuration
@EnableConfigurationProperties(RouterGroupProperties.class)
public class DataSourceAutoConfig {

    @Bean
    public Map<StrategyType, RouterStrategy> routerStrategiesSDT() {
        Map<StrategyType, RouterStrategy> routerStrategiesSDT = new HashMap<>();
        for (StrategyType strategyType : StrategyType.values()) {
            routerStrategiesSDT.put(strategyType, RouterStrategyFactory.getInstance(strategyType));
        }
        return routerStrategiesSDT;
    }

    /**
     * 创建AOP切面Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public SplitDTAspect dbRouterJoinPoint(RouterGroupProperties routerGroupProperties,
                                           Map<StrategyType, RouterStrategy> routerStrategiesSDT) {
        return new SplitDTAspect(routerGroupProperties, routerStrategiesSDT);
    }



    /**
     * 创建MyBatis插件Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public Interceptor dynamicMybatisPlugin(RouterGroupProperties routerGroupProperties,
                                            Map<StrategyType, RouterStrategy> routerStrategiesSDT) {
        return new DynamicMybatisPlugin(routerGroupProperties, routerStrategiesSDT);
    }

    /**
     * 创建动态数据源Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(RouterGroupProperties properties) {
        // 创建数据源Map
        Map<Object, Object> targetDataSources = new HashMap<>();

        // 遍历配置，创建每个数据源
        Map<String, DataSourceGroup> groups = properties.getGroups();
        for (String groupKey : groups.keySet()) {

            DataSourceGroup dataSourceGroup = groups.get(groupKey);
            Map<String, DataSourceConfig> configs = dataSourceGroup.getDataSource();
            for (String dbKey : configs.keySet()) {

                DataSourceConfig dataSourceConfig = configs.get(dbKey);

                // 使用DriverManagerDataSource创建数据源
                DriverManagerDataSource dataSource = new DriverManagerDataSource();
                dataSource.setDriverClassName(dataSourceConfig.getDriverClassName());
                dataSource.setUrl(dataSourceConfig.getUrl());
                dataSource.setUsername(dataSourceConfig.getUsername());
                dataSource.setPassword(dataSourceConfig.getPassword());

                targetDataSources.put(groupKey+dbKey, dataSource);
            }

        }

        // 创建动态数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);

        // 设置默认数据源（使用第一个）
        if (!targetDataSources.isEmpty()) {
            DataSourceConfig dataSourceConfig = groups.get("group01").getDataSource().get("db01");
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(dataSourceConfig.getDriverClassName());
            dataSource.setUrl(dataSourceConfig.getUrl());
            dataSource.setUsername(dataSourceConfig.getUsername());
            dataSource.setPassword(dataSourceConfig.getPassword());
            dynamicDataSource.setDefaultTargetDataSource(dataSource);
        }

        return dynamicDataSource;
    }

    /**
     * 创建事务管理器Bean（供 @Transactional 使用）
     */
    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 创建 TransactionTemplate（编程式事务）
     * 复用 Spring 容器已有的事务管理器
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }


}
