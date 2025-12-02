package com.wht.sdt.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 数据库路由配置属性
 * 从application.yml或application.properties中读取配置
 *
 * @author wht
 */
@Data
@ConfigurationProperties(prefix = "split-database-table.sdt.router")
public class RouterGroupProperties {

    private Map<String, DataSourceGroup> groups;

}
