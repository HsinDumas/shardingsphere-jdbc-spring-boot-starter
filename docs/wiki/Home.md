# shardingsphere-jdbc-spring-boot-starter Wiki

欢迎来到项目 Wiki。

这个 starter 面向 Spring Boot 3/4，目标是让 ShardingSphere JDBC 在业务项目中更轻量地落地。

## 快速入口

1. [Quick Start](quick-start.md)
2. [Configuration Examples](configuration.md)
3. [Release Guide](../release.md)

## 关键设计约定

1. 默认保守策略：只有在容器里不存在业务侧 `DataSource` Bean 时，starter 才会自动创建分片数据源。
2. 显式启用：请设置 `shardingsphere.enabled=true`。
3. 构建统一：使用 JDK 25 toolchain 构建，产物目标兼容 JDK 17+。

## 适用场景

1. 希望快速接入分库分表且减少手写装配代码。
2. 使用 Spring Boot 3.4.x 或 4.0.x。
3. 需要与现有业务 `DataSource` 共存，并且避免 starter 抢占。

## 不适用场景

1. 你希望 starter 覆盖业务已存在的普通 `DataSource`。
2. 你需要完全自定义 `DataSource` 装配流程（建议自行创建并注入 ShardingSphereDataSource）。

## 常见问题

### 为什么配置了 starter 但分库分表没生效？

通常是因为业务已经定义了普通 `DataSource` Bean，starter 按保守策略自动让出。

### 同时兼容 Boot 3 和 4 吗？

可以。CI 会对 Boot 3 与 Boot 4 做矩阵构建验证。

---

如果你在使用中遇到问题，欢迎提交 Issue 或 PR。
