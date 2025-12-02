# Split Database & Table Router Starter 使用文档

![Version](https://img.shields.io/badge/version-2.0.0-blue)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen)
![MyBatis](https://img.shields.io/badge/MyBatis-3.0.3-red)

## 目录

- [项目简介](#项目简介)
  - [核心特性](#核心特性)
  - [项目架构](#项目架构)
  - [技术栈](#技术栈)
- [适用场景](#适用场景)
- [快速开始](#快速开始)
- [详细使用说明](#详细使用说明)
- [核心组件说明](#核心组件说明)
- [工作原理](#工作原理)
- [数据库表设计](#数据库表设计)
- [高级配置](#高级配置)
- [最佳实践](#最佳实践)
- [性能优化](#性能优化)
- [故障排查](#故障排查)
- [常见问题FAQ](#常见问题faq)
- [版本历史](#版本历史)
- [构建与部署](#构建与部署)
- [技术支持](#技术支持)
- [许可证](#许可证)
- [贡献指南](#贡献指南)
- [附录](#附录)

## 项目简介

`split-dbt-router-starter` 是一个基于Spring Boot的数据库分库分表路由组件，提供了灵活的分库分表策略，支持多数据源动态路由和SQL表名动态替换功能。

### 核心特性

- ✅ **多数据源支持**：支持多个数据源组配置
- ✅ **灵活的分库分表策略**：
  - 一致性哈希算法（CONSISTENT_HASH）
  - 标准哈希算法（HASH）
  - 基于时间的路由（TIME_BASED）
- ✅ **AOP切面拦截**:通过注解方式简化使用
- ✅ **MyBatis集成**：自动拦截SQL并替换表名
- ✅ **Spring Boot自动配置**：开箱即用

### 项目架构

本项目采用模块化设计，主要包含以下核心模块：

```
split-dbt-router-starter
├── annotation          # 注解定义
│   └── @SplitDT       # 分库分表注解
├── aspect             # AOP切面
│   └── SplitDTAspect  # 路由切面拦截器
├── config             # 配置管理
│   ├── DataSourceAutoConfig        # 自动配置类
│   ├── DynamicDataSource          # 动态数据源
│   └── DynamicMybatisPlugin       # MyBatis拦截器
├── context            # 上下文管理
│   ├── SDTContext          # ThreadLocal上下文
│   └── StrategyContext     # 策略上下文
├── enumeration        # 枚举定义
│   └── StrategyType        # 路由策略类型
├── properties         # 配置属性
│   ├── RouterGroupProperties   # 路由配置
│   ├── DataSourceGroup        # 数据源组配置
│   └── DataSourceConfig       # 数据源配置
├── strategy           # 路由策略
│   ├── RouterStrategy              # 策略接口
│   ├── RouterStrategyFactory       # 策略工厂
│   └── impl
│       ├── RouterStrategyHashCode      # 标准哈希策略
│       ├── RouterStrategyConsistentHash # 一致性哈希策略
│       └── RouterStrategyTimeBased     # 时间路由策略
└── util               # 工具类
    └── SqlTableReplacer    # SQL表名替换工具
```

### 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 基础语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring AOP | 3.2.0 | 切面编程 |
| MyBatis | 3.0.3 | ORM框架 |
| Druid | 1.2.20 | 连接池 |
| JSqlParser | 4.5 | SQL解析 |
| Lombok | 1.18.30 | 代码简化 |


---

## 适用场景

### 什么时候需要分库分表？

本组件适合以下场景：

✅ **数据量大**
- 单表数据量超过500万行
- 数据持续快速增长
- 查询性能明显下降

✅ **高并发写入**
- 用户量大，写入并发高
- 需要横向扩展数据库写能力
- 单库连接数接近瓶颈

✅ **业务可拆分**
- 数据可以按某个维度（如用户ID、订单ID）拆分
- 大部分查询都带有路由字段
- 跨分片查询需求较少

### 典型应用场景

- 📱 **电商系统**：订单表、用户表按用户ID分片
- 💬 **社交平台**：消息表、动态表按用户ID分片
- 🎮 **游戏系统**：玩家数据、日志表按玩家ID分片
- 📊 **日志系统**：操作日志、审计日志按时间分片
- 💰 **金融系统**：交易记录、账户流水按账户ID分片

### 不适合的场景

❌ **数据量小**：单表数据量小于100万，不建议分库分表，增加系统复杂度
❌ **频繁跨片查询**：业务需要大量JOIN、聚合查询
❌ **无明确路由字段**：数据无法按某个字段均匀分布

---

## 快速开始

### 1. 添加依赖

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.wht.sdt</groupId>
    <artifactId>split-dbt-router-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 2. 配置文件

在 `application.yml` 中添加数据源路由配置：

```yaml
# 数据库路由配置
split-database-table:
  db:
    router:
      groups:
        # 组1：单数据源（不分库分表）
        group01:
          enable-split: false
          data-source:
            db01:
              driver-class-name: com.mysql.cj.jdbc.Driver
              url: jdbc:mysql://localhost:3306/lottery?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
              username: root
              password: password123
              
        # 组2：分库分表配置
        group02:
          # 启用分库分表
          enable-split: true
          # 分库数量
          db-count: 2
          # 默认路由字段
          router-key: uId
          # 分表数量配置（表名: 分表数量）
          tb-counts:
            user_strategy_export: 4
            tableB: 
            ...
          # 数据源配置
          data-source:
            # 第一个数据库
            db01:
              driver-class-name: com.mysql.cj.jdbc.Driver
              url: jdbc:mysql://localhost:3306/lottery_01?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
              username: root
              password: password123
            # 第二个数据库
            db02:
              driver-class-name: com.mysql.cj.jdbc.Driver
              url: jdbc:mysql://localhost:3306/lottery_02?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
              username: root
              password: password123
            db03:
              ...
        group03:
            ... 

# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.domain  # 根据你的项目包名配置
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true

# 日志配置（可选）
logging:
  level:
    com.wht.sdt: DEBUG  # 组件内部日志
    com.example: DEBUG  # 你的应用日志
```

---

## 详细使用说明

### 1. 基本使用

在需要进行分库分表的Mapper方法上添加 `@SplitDT` 注解：

```java
package com.example.dao;

import com.wht.sdt.annotation.SplitDT;
import com.wht.marketing.domain.UserStrategyExport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface tableAMapper {

    /**
     * 插入用户策略导出记录
     * @param record 用户策略导出对象
     */
    @SplitDT(groupKey = 2, routeKey = "id", strategy = StrategyType.HASH)
    void insert(Entity record);

    /**
     * 根据ID查询
     * @param id ID
     */
    @SplitDT(groupKey = 2, routeKey = "id", strategy = StrategyType.HASH)
    Entity selectById(Entity record);
}
```

### 2. @SplitDT 注解参数说明

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `groupKey` | int | 1 | 数据源组编号，对应配置文件中的group编号 |
| `routeKey` | String | "" | 路由字段名称，为空时使用配置文件中的默认字段 |
| `strategy` | StrategyType | HASH | 路由策略类型 |

#### groupKey 说明

- `groupKey = 1` 对应配置文件中的 `group01`
- `groupKey = 2` 对应配置文件中的 `group02`
- 以此类推...

#### routeKey 说明

- 指定用于分库分表计算的字段名
- 必须是方法参数对象中的属性名
- 如果为空，则使用配置文件中 `router-key` 的值

#### strategy 策略类型

支持三种路由策略：

1. **HASH（标准哈希）**
   - 使用Java的 `hashCode()` 方法进行哈希计算
   - 适合大多数场景
   
2. **CONSISTENT_HASH（一致性哈希）**
   - 使用一致性哈希算法
   - 适合需要数据迁移、扩容的场景
   
3. **TIME_BASED（基于时间）**
   - 根据时间戳进行路由
   - 适合按时间维度分表的场景

---


## 核心组件说明

### 1. 核心注解

#### @SplitDT
- **作用**：标记需要分库分表的方法
- **位置**：Mapper接口的方法上
- **参数**：groupKey, routeKey, strategy

### 2. 核心配置类

#### RouterGroupProperties
- **作用**：数据源路由配置属性
- **配置前缀**：`split-database-table.db.router`

#### DataSourceGroup
- **作用**：数据源组配置
- **主要属性**：
  - `enableSplit`: 是否启用分库分表
  - `dbCount`: 分库数量
  - `routerKey`: 默认路由字段
  - `tbCounts`: 表分片数量配置
  - `dataSource`: 数据源配置

### 3. 核心组件

#### DynamicDataSource
- **作用**：动态数据源，根据ThreadLocal中的上下文动态切换数据源
- **继承**：Spring的 `AbstractRoutingDataSource`

#### DynamicMybatisPlugin
- **作用**：MyBatis拦截器插件，拦截SQL执行并动态替换表名
- **拦截点**：`StatementHandler.prepare`

#### SplitDTAspect
- **作用**：AOP切面，拦截带有 `@SplitDT` 注解的方法
- **功能**：
  - 解析注解参数
  - 从方法参数中提取路由字段值
  - 执行路由策略
  - 设置ThreadLocal上下文

### 4. 路由策略

#### RouterStrategy 接口
```java
public interface RouterStrategy {
    /**
     * 分库路由
     */
    void splitDB(StrategyContext strategyContext);
    
    /**
     * 分表路由
     */
    void splitTB(StrategyContext strategyContext);
}
```

#### 内置策略实现

1. **RouterStrategyHashCode**（标准哈希策略）
   - 分库计算：`dbIdx = abs(hashCode) % dbCount + 1`
   - 分表计算：`tbIdx = abs(hashCode) % tbCount + 1`

2. **RouterStrategyConsistentHash**（一致性哈希策略）
   - 使用虚拟节点实现一致性哈希
   - 适合动态扩容场景

3. **RouterStrategyTimeBased**（基于时间策略）
   - 根据时间戳进行路由
   - 适合按时间维度分表

---

## 工作原理

### 执行流程

```
1. 方法调用（带@SplitDT注解）
   ↓
2. SplitDTAspect切面拦截
   ↓
3. 解析注解参数（groupKey, routeKey, strategy）
   ↓
4. 从方法参数中提取路由字段值
   ↓
5. 执行路由策略（splitDB）
   ↓
6. 设置ThreadLocal上下文（GroupKey, DBKey, TBKey）
   ↓
7. 执行目标方法（Mapper方法）
   ↓
8. MyBatis执行SQL
   ↓
9. DynamicMybatisPlugin拦截
   ↓
10. 执行分表策略（splitTB）
   ↓
11. 替换SQL中的表名（如：user_strategy_export → user_strategy_export_001）
   ↓
12. DynamicDataSource切换数据源
   ↓
13. 执行替换后的SQL
   ↓
14. 清除ThreadLocal上下文
```

### ThreadLocal上下文

#### SDTContext（Split Database Table Context）

```java
public class SDTContext {
    // 数据源组Key（如：group01）
    private static ThreadLocal<String> gKey;
    
    // 数据库Key（如：db01）
    private static ThreadLocal<String> dbKey;
    
    // 分表Key（如：001）
    private static ThreadLocal<String> tbKey;
    
    // 路由策略类型
    private static ThreadLocal<StrategyType> strategyType;
}
```


## 高级配置

### 1. 自定义路由策略

实现 `RouterStrategy` 接口：

```java
package com.example.strategy.impl;

import com.wht.sdt.context.SDTContext;
import com.wht.sdt.context.StrategyContext;
import com.wht.sdt.strategy.RouterStrategy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomRouterStrategy implements RouterStrategy {

    @Override
    public void splitDB(StrategyContext strategyContext) {
        // 自定义分库逻辑
        String keyValue = strategyContext.getKeyValue();
        int dbCount = strategyContext.getDbCount();

        // 实现你的路由算法
        int dbIdx = customAlgorithm(keyValue, dbCount);

        String dbKey = "db" + String.format("%02d", dbIdx);
        SDTContext.setDBKey(dbKey);

        log.debug("Custom DB routing: key={}, dbKey={}", keyValue, dbKey);
    }

    @Override
    public void splitTB(StrategyContext strategyContext) {
        // 自定义分表逻辑
        String keyValue = strategyContext.getKeyValue();
        int tbCount = strategyContext.getTbCount();

        // 实现你的路由算法
        int tbIdx = customAlgorithm(keyValue, tbCount);

        String tbKey = String.format("%03d", tbIdx);
        SDTContext.setTBKey(tbKey);

        log.debug("Custom TB routing: key={}, tbKey={}", keyValue, tbKey);
    }

    private int customAlgorithm(String key, int count) {
        // 你的自定义算法
        return 1;
    }
}
```

### 2. 注册自定义策略

```java
package com.example.config;

import com.wht.sdt.strategy.RouterStrategy;
import com.example.strategy.impl.CustomRouterStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomStrategyConfig {

    @Bean
    public RouterStrategy customRouterStrategy(Map<StrategyType, RouterStrategy> routerStrategiesSDT) {
      routerStrategiesSDT.put(StrategyType.CUSTOM, new CustomRouterStrategy()); 
      return new CustomRouterStrategy();
    }

}
```

---

## 最佳实践

### 1. 路由字段选择

✅ **推荐**：
- 使用业务主键（如：userId, orderId）
- 分布均匀的字段
- 不经常变化的字段

❌ **不推荐**：
- 状态字段（分布不均）
- 时间字段（除非使用TIME_BASED策略）
- 可能为空的字段

### 2. 分库分表数量规划

- **分库数量**：建议2的幂次方（2, 4, 8, 16...）
- **分表数量**：根据数据量评估，每表建议不超过500万行
- **总表数量**：分库数 × 每库分表数

### 3. 配置建议

```yaml
split-database-table:
  db:
    router:
      groups:
        group02:
          enable-split: true
          db-count: 4              # 4个数据库
          router-key: userId
          tb-counts:
            order_info: 16         # 每个库16张表，总共64张表
            user_account: 8        # 每个库8张表，总共32张表
```

### 4. 注意事项

⚠️ **重要提示**：

1. **跨库事务问题**
   - 分库分表后无法使用数据库事务
   - 需要使用分布式事务方案（如Seata）

2. **跨库查询问题**
   - 避免跨分片的JOIN查询
   - 考虑数据冗余或使用聚合层

3. **路由字段必须存在**
   - 所有操作必须包含路由字段
   - 无路由字段的查询会导致全表扫描

4. **ThreadLocal清理**
   - 框架会自动清理ThreadLocal
   - 如果手动操作，记得调用 `SDTContext.clearAll()`

---

## 性能优化

### 1. 连接池配置

```yaml
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
```

### 2. MyBatis配置优化

```yaml
mybatis:
  configuration:
    cache-enabled: true                # 启用二级缓存
    lazy-loading-enabled: true         # 启用延迟加载
    aggressive-lazy-loading: false     # 关闭积极的延迟加载
    default-executor-type: REUSE       # 复用PreparedStatement
```

---

## 故障排查

### 1. 数据源未切换

**问题**：SQL执行在错误的数据库

**排查步骤**：
1. 检查 `@SplitDT` 注解配置
2. 启用DEBUG日志查看路由信息
3. 确认配置文件中的groupKey配置

```yaml
logging:
  level:
    com.wht.sdt: DEBUG  # 组件内部日志
    com.example: DEBUG  # 你的应用日志
```

### 2. 表名未替换

**问题**：SQL未替换表名后缀

**排查步骤**：
1. 检查配置中的 `tb-counts` 是否包含该表
2. 检查 `enable-split` 是否为true
3. 查看日志确认MyBatis插件是否生效

### 3. 路由字段值为空

**问题**：报错"数据库路由key属性值为空"

**解决方案**：
1. 确保方法参数对象包含路由字段
2. 路由字段不能为null或空字符串
3. 检查字段名称是否拼写正确

---

## 常见问题FAQ

### Q1: 是否支持读写分离？
**A**: 当前版本不直接支持读写分离，可以通过配置多个数据源组实现。

### Q2: 如何进行数据迁移？
**A**: 建议使用一致性哈希策略（CONSISTENT_HASH），便于后续扩容。

### Q3: 是否支持动态添加数据源？
**A**: 当前需要重启应用，后续版本会支持动态配置。

### Q4: 如何处理全局查询？
**A**: 使用不分库分表的组（enable-split: false），或在应用层聚合多个分片的结果。

### Q5: 支持哪些数据库？
**A**: 理论上支持所有JDBC数据库，已测试MySQL。其他数据库（PostgreSQL、Oracle、SQL Server）理论上也支持，但需要自行测试验证。

### Q6: 分库分表后如何进行分页查询？
**A**: 建议在应用层实现分页逻辑。如果数据量大，考虑使用搜索引擎（如Elasticsearch）作为查询层。

### Q7: 可以只分表不分库吗？
**A**: 可以，设置 `db-count: 1` 即可只分表不分库。

### Q8: 如何保证分库分表后的唯一ID？
**A**: 建议使用分布式ID生成方案，如：雪花算法（Snowflake）、数据库号段模式、Redis自增等。

### Q9: 路由策略可以混合使用吗？
**A**: 可以，不同的Mapper方法可以使用不同的路由策略，通过 `@SplitDT` 注解的 `strategy` 参数指定。

### Q10: 多个表使用同一路由字段时如何配置？
**A**: 在配置文件中设置 `router-key` 为默认路由字段，然后在 `tb-counts` 中配置各表的分表数量即可。

---

## 版本历史

### v2.0.0 (2025-12-02)
- ✨ 正式版本发布
- ✅ 支持Spring Boot 3.2.0
- ✅ 支持Java 17+
- ✅ 优化路由策略性能
- ✅ 改进SQL解析逻辑
- ✅ 增强日志输出


---

## 构建与部署

### 本地构建

```bash
# 克隆项目
git clone https://github.com/wht/split-dbt-router-starter.git
cd split-dbt-router-starter

# 编译打包
mvn clean install

# 跳过测试打包
mvn clean install -DskipTests

# 生成源码和文档
mvn clean package source:jar javadoc:jar
```

### 安装到本地仓库

```bash
mvn clean install
```

构建成功后，将在 `target` 目录生成以下文件：
- `split-dbt-router-starter-2.0.0.jar` - 主jar包
- `split-dbt-router-starter-2.0.0-sources.jar` - 源码jar包
- `split-dbt-router-starter-2.0.0-javadoc.jar` - 文档jar包

---

## 技术支持

- **项目地址**：[GitHub Repository]
- **问题反馈**：[Issue Tracker]
- **作者**：wht

---


---

## 附录

### A. 完整配置示例

参考 `src/main/resources/application-example.yml` 文件。

### B. Spring Boot自动配置

框架通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 实现自动配置。

### C. 依赖列表

核心依赖及版本：

- **Spring Boot**: 3.2.0
- **MyBatis Spring Boot Starter**: 3.0.3
- **MySQL Connector**: 8.2.0
- **Druid Connection Pool**: 1.2.20
- **JSqlParser**: 4.5
- **Lombok**: 1.18.30
- **Apache Commons BeanUtils**: 1.9.4

最低要求：
- **Java**: 17+
- **Maven**: 3.6+

---

**最后更新时间**: 2025-12-02
