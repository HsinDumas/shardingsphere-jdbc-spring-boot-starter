# shardingsphere-jdbc-spring-boot-starter

[English](README_EN.md)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![JDK](https://img.shields.io/badge/JDK-17+-4EB1BA.svg)](https://docs.oracle.com/en/java/javase/17/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x%20%7C%204.x-green.svg)](https://docs.spring.io/spring-boot/docs/current/reference/html/)

让 ShardingSphere JDBC 在 Spring Boot 3/4 里更好接入，少一点胶水代码，多一点可维护性。

## Why This Project

1. 官方 Starter 停更后，仍可在 Spring Boot 3/4 项目里快速接入 ShardingSphere JDBC。
2. 默认保守策略（显式启用 + DataSource 自动让出），降低存量系统改造风险。
3. 配置路径清晰，常见分库分表场景可直接参考示例上手。
4. 支持范围明确：支持 Spring Boot 3.x / 4.x。
5. 产物兼容 JDK 17+，大多数现有运行环境可直接使用。

## Compatibility

| starter | shardingsphere-jdbc | Spring Boot (Supported) |
| --- | --- | --- |
| 1.0.4 | 5.5.2 | 3.x / 4.x (Legacy) |
| 1.1.0 | 5.5.3 | 3.x / 4.x |

## Quick Start

### 1) 引入依赖

```xml
<dependency>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>shardingsphere-jdbc-spring-boot-starter</artifactId>
  <version>1.1.0</version>
</dependency>

<!-- 应用侧需要显式提供 JDBC 运行时依赖 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
</dependency>
```

说明：本 starter 不会传递业务 JDBC 驱动，请在应用侧按实际数据库类型引入对应驱动。

### 2) 显式启用

```yaml
shardingsphere:
  enabled: true
```

默认不抢占 `DataSource`。如果业务已定义 `DataSource` Bean，本 starter 会自动让出。

这是一种保守策略：当容器中已存在业务侧普通 `DataSource` 时，本 starter 不会再创建分片 `DataSource`，因此分库分表能力不会由本 starter 接管。

### 推荐用法

1. 想让本 starter 自动接管分片：不要再额外定义普通 `DataSource` Bean。
2. 想完全自定义 `DataSource`：请手动创建并注入 `ShardingSphereDataSource`，不要依赖本 starter 自动装配。

### 3) 最小分表示例

```yaml
shardingsphere:
  enabled: true
  data-sources:
    - jdbc-url: jdbc:mysql://127.0.0.1:3306/test
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
      max-pool-size: 20
      min-idle: 5
      idle-timeout: 60000
      connection-test-query: SELECT 1
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
  tables:
    - logic-table: t_order
      actual-data-nodes: ds_${0..0}.t_order_${0..1}
      table-strategy:
        algorithm-type: inline
        sharding-column: order_id
        props:
          algorithm-expression: t_order_${order_id % 2}
```

## Configuration Examples

### 分库

```yaml
shardingsphere:
  enabled: true
  data-sources:
    - jdbc-url: jdbc:mysql://db0:3306/test
      username: xxx
      password: yyy
      driver-class-name: com.mysql.cj.jdbc.Driver
    - jdbc-url: jdbc:mysql://db1:3306/test
      username: xxx
      password: yyy
      driver-class-name: com.mysql.cj.jdbc.Driver
  tables:
    - logic-table: t_order
      actual-data-nodes: ds_${0..1}.t_order
      database-strategy:
        algorithm-type: inline
        sharding-column: user_id
        props:
          algorithm-expression: ds_${Math.abs(user_id.hashCode()) % 2}
```

### 分库 + 分表

```yaml
shardingsphere:
  enabled: true
  data-sources:
    - jdbc-url: jdbc:mysql://db0:3306/test
      username: xxx
      password: yyy
      driver-class-name: com.mysql.cj.jdbc.Driver
    - jdbc-url: jdbc:mysql://db1:3306/test
      username: xxx
      password: yyy
      driver-class-name: com.mysql.cj.jdbc.Driver
  tables:
    - logic-table: t_order
      actual-data-nodes: ds_${0..1}.t_order_${0..15}
      database-strategy:
        algorithm-type: inline
        sharding-column: user_id
        props:
          algorithm-expression: ds_${Math.abs(user_id.hashCode()) % 2}
      table-strategy:
        algorithm-type: inline
        sharding-column: order_id
        props:
          algorithm-expression: t_order_${Math.abs(order_id.hashCode()) % 16}
```

### CLASS_BASED 算法

```yaml
shardingsphere:
  enabled: true
  tables:
    - logic-table: t_order
      actual-data-nodes: ds_${0..1}.t_order_${0..1}
      table-strategy:
        algorithm-type: class_based
        sharding-column: order_id
        props:
          strategy: STANDARD
          algorithmClassName: your.package.YourStandardShardingAlgorithm
```

## Build

```bash
./gradlew clean build
./gradlew test
```

构建策略：

1. 统一使用 Java 25 toolchain 编译。
2. 统一使用 `--release 17`，保证产物可运行在 JDK 17 及以上。
3. 本地构建建议使用 JDK 25（或启用 Gradle toolchain 自动下载），运行时最低为 JDK 17。

按 Spring Boot 版本验证：

```bash
# Boot 3
./gradlew clean test -PspringBootVersion=3.5.16

# Boot 4
./gradlew clean test -PspringBootVersion=4.1.0
```

## Release

项目使用 Gradle + GitHub Actions 的 tag 驱动发布。

完整发布流程请见 [docs/release.md](docs/release.md)。

Wiki 索引请见 [docs/wiki/README.md](docs/wiki/README.md)。

Wiki English index: [docs/wiki/README_EN.md](docs/wiki/README_EN.md).

## Upgrade Notes (5.5.x)

1. 当前 starter 内置对齐 `shardingsphere-jdbc` 5.5.3，建议避免业务侧重复引入不同版本。
2. 官方 5.3.0+ 已移除 Spring Boot Starter 形态，可继续使用本 starter，或改走官方 JDBC Driver 方案。
3. 升级时重点回归分片算法 props 和 SQL 路由结果。

## Contributing

欢迎 Issue 和 PR。

如果这个项目帮到了你，欢迎点个 Star。

## License

[Apache License 2.0](LICENSE)