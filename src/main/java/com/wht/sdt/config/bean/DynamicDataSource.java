package com.wht.sdt.config.bean;

import com.wht.sdt.context.SDTContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * 根据上下文中的数据库键动态切换数据源
 *
 * @author wht
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {


    @Override
    protected Object determineCurrentLookupKey() {

        if (SDTContext.getGKey() == null || "".equals(SDTContext.getGKey())
                || SDTContext.getDBKey() == null || "".equals(SDTContext.getDBKey())) {
            return "group01db01";
        } else {
            // 从ThreadLocal获取当前线程的数据库键
            // 格式: group01db01 (getGKey()="group01", getDBKey()="db01")
            // 注意：DBKey已经包含"db"前缀，由路由策略设置
            log.debug(SDTContext.getGKey() + SDTContext.getDBKey());
            return SDTContext.getGKey() + SDTContext.getDBKey();
        }
    }
}
