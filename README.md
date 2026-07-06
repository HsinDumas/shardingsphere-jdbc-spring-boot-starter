# shardingsphere-jdbc-spring-boot-starter

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![JDK](https://img.shields.io/badge/JDK-17+-4EB1BA.svg)](https://docs.oracle.com/en/java/javase/17/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x%20%7C%204.x-green.svg)](https://docs.spring.io/spring-boot/docs/current/reference/html/)

让 ShardingSphere JDBC 在 Spring Boot 3 里更好接入，少一点胶水代码，多一点可维护性。

这个项目的目标很明确：

1. 给官方 Starter 停更后的 Spring Boot 3 用户一个可用选项。
2. 保持配置简单，覆盖常见分库分表场景。
3. 默认不侵入业务 DataSource（显式启用，支持业务侧覆盖）。

## Compatibility

| starter | shardingsphere-jdbc | Spring Boot |
| --- | --- | --- |
| 1.0.4 | 5.5.2 | 3.4.x |
| 1.0.4 | 5.5.2 | 4.0.x |

## Quick Start

### 1) 引入依赖

```xml
<dependency>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>shardingsphere-jdbc-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

### 2) 显式启用

```yaml
shardingsphere:
  enabled: true
```

默认不抢占 `DataSource`。如果业务已定义 `DataSource` Bean，本 starter 会自动让出。

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

按 Spring Boot 版本验证：

```bash
# Boot 3
./gradlew clean test -PspringBootVersion=3.4.8

# Boot 4
./gradlew clean test -PspringBootVersion=4.0.0
```

## Release

项目使用 Gradle + GitHub Actions 的 tag 驱动发布。

```bash
# 查看版本
./gradlew printVersion

# 发布 snapshot 到 Maven Central snapshot 仓库
./gradlew publishSnapshotToMavenCentral

# CI tag 发布到 Maven Central（本地一般不直接跑）
./gradlew -PreleaseVersion=1.0.4 publishReleaseToMavenCentral
```

正式发布方式：

```bash
git tag v1.0.4
git push origin v1.0.4
```

发布成功后，workflow 会自动把 `gradle.properties` 里的 `projectVersion` bump 到下一个 `-SNAPSHOT`。

## Upgrade Notes (5.5.x)

1. 当前 starter 内置对齐 `shardingsphere-jdbc` 5.5.2，建议避免业务侧重复引入不同版本。
2. 官方 5.3.0+ 已移除 Spring Boot Starter 形态，可继续使用本 starter，或改走官方 JDBC Driver 方案。
3. 升级时重点回归分片算法 props 和 SQL 路由结果。

## Contributing

欢迎 Issue 和 PR。

如果这个项目帮到了你，欢迎点个 Star。

## License

[Apache License 2.0](LICENSE)