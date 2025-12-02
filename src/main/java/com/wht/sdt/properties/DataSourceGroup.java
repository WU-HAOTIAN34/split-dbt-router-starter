package com.wht.sdt.properties;


import lombok.Data;

import java.util.Map;

@Data
public class DataSourceGroup {

    /**
     * 分库数量
     */
    private boolean enableSplit;
    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private Map<String, Integer> tbCounts;

    /**
     * 路由字段
     */
    private String routerKey;

    /**
     * 数据源配置列表
     * key: db01, db02, ...
     * value: 数据源配置
     */
    private Map<String, DataSourceConfig> dataSource;


    public boolean getEnableSplit() {
        return enableSplit;
    }

}
