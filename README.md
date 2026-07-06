# shardingsphere-jdbc-spring-boot-starter

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![License](https://img.shields.io/badge/JDK-17+-4EB1BA.svg)](https://docs.oracle.com/en/java/javase/17/)
[![License](https://img.shields.io/badge/SpringBoot-3+-green.svg)](https://docs.spring.io/spring-boot/docs/current/reference/html/)
[![Author](https://img.shields.io/badge/Author-HsinDumas-orange.svg?style=flat-square)](https://github.com/HsinDumas)

**适用于 shardingsphere-jdbc 5.5.2 的 spring-boot-starter (Spring Boot 3)**。

1. 由于官方提供的 starter 停止维护，因此本项目基于 shardingsphere-jdbc 进行一定的配置封装
2. 目前只适配了 Spring Boot 3.x
3. 当前实现对应 shardingsphere-jdbc 5.5.2
4. 使用 hikari 作为数据库连接池

> 🚀项目持续优化迭代，欢迎大家提ISSUE和PR！麻烦大家能给一颗star✨，您的star是我们持续更新的动力！

## 版本对应关系

| starter 版本 | shardingsphere-jdbc 版本 | Spring Boot |
| --- | --- | --- |
| 1.0.4 | 5.5.2 | 3.x |

## 快速开始

### 启用自动装配

从 `1.0.5-SNAPSHOT` 开始，starter 默认不抢占 `DataSource`。需要显式开启：

```yaml
shardingsphere:
  enabled: true
```

如果业务已自行声明 `DataSource` Bean，本 starter 会自动让出（不覆盖业务 Bean）。

### 引入依赖

```xml

<dependency>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>shardingsphere-jdbc-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

### 本地构建（Gradle）

```bash
./gradlew clean build
```

### 版本与发布（参考 stagger）

默认版本通过 `releaseVersion` 或 `RELEASE_VERSION` 注入；未提供时使用 `gradle.properties` 中的 `projectVersion`（当前为 `1.0.5-SNAPSHOT`）。

```bash
# 查看当前版本
./gradlew printVersion

# 发布到 Maven Central Snapshot 仓库
./gradlew publishSnapshotToMavenCentral

# 发布到 Maven Central（仅允许在 CI tag 环境下执行）
./gradlew -PreleaseVersion=1.0.4 publishReleaseToMavenCentral
```

### CI 与正式发布

项目已切换为 Gradle 构建，并使用 GitHub Actions：

```text
.github/workflows/ci.yml       # push / PR 自动执行 ./gradlew clean build
.github/workflows/release.yml  # push tag vX.Y.Z 自动发布到 Maven Central 并生成 GitHub Release Notes
```

正式发布建议使用 tag 驱动：

```bash
git tag v1.0.4
git push origin v1.0.4
```

发布工作流会在成功后自动把 `gradle.properties` 的 `projectVersion` bump 为下一个 patch 的 `-SNAPSHOT`，并提交回 `main` 分支。

## 升级到 5.5.x 注意事项

1. 本 starter 当前内置并对齐 `shardingsphere-jdbc` 5.5.2，建议避免在业务侧重复显式引入其他版本的 `shardingsphere-jdbc`，以免出现依赖冲突。
2. 从官方 5.3.0 开始，Spring Boot Starter 形态已移除，建议继续使用本 starter 或改用官方 JDBC Driver 方案（`jdbc:shardingsphere:classpath:xxx.yaml`）。
3. 若业务从 5.2.x 升级，请重点回归验证分片算法配置（尤其是 INLINE 与 CLASS_BASED 的 props）和 SQL 路由结果。

### 配置属性

#### 分表

``` yaml
shardingsphere:
  data-sources:
    - jdbc-url: jdbc:mysql://...
      username: xxx
      password: yyy
      driver-class-name: com.mysql.cj.jdbc.Driver
      max-pool-size: 20
      idle-timeout: 60000
      min-idle: 5
      connection-test-query: SELECT 1
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
      ...
  tables:
    - logic-table: test_table
      actual-data-nodes: test_table_${0..9}
      table-strategy:
        algorithm-type: inline
        sharding-column: test_column
        props:
          algorithm-expression: test_${Math.abs(test_column.hashCode()) % 10}
```

#### 分库

本插件会按照配置的顺序，自动为 datasource 加上 `ds_` 的前缀作为库的唯一标识。
相应的在配置算法时也要使用 `ds_0`、`ds_1`... 来表示不同的库。

``` yaml
shardingsphere:
  data-sources:
    - jdbc-url: jdbc:mysql://...
      username: xxx
      password: yyy
      ...
    - jdbc-url: jdbc:mysql://...
      username: xxx
      password: yyy
      ...
  tables:
    - logic-table: test_table
      actual-data-nodes: ds_${0..1}.test_table
      database-strategy:
        algorithm-type: inline
        sharding-column: test_column
        props:
          algorithm-expression: ds_${Math.abs(test_column.hashCode()) % 2}
```

#### 分库+分表

``` yaml
shardingsphere:
  data-sources:
    - jdbc-url: jdbc:mysql://...
      username: xxx
      password: yyy
      ...
    - jdbc-url: jdbc:mysql://...
      username: xxx
      password: yyy
      ...
  tables:
    - logic-table: test_table
      actual-data-nodes: ds_${0..1}.test_table_${0..9}
      table-strategy:
        algorithm-type: inline
        sharding-column: test_column
        props:
          algorithm-expression: test_${Math.abs(test_column.hashCode()) % 10}
      database-strategy:
        algorithm-type: inline
        sharding-column: test_column
        props:
          algorithm-expression: ds_${Math.abs(test_column.hashCode()) % 2}
```

#### 除了 INLINE 还支持 CLASS_BASED

具体 props 请看源码 org.apache.shardingsphere.sharding.algorithm.sharding.classbased.ClassBasedShardingAlgorithm

``` yaml
shardingsphere:
  ...
  tables:
    - table-strategy:
        algorithm-type: class_based
        sharding-column: test_column
        props:
          strategy: STANDARD
          algorithmClassName: org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm...
          xxx: yyy
```