# shardingsphere-jdbc-spring-boot-starter

[中文](README.md)

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![JDK](https://img.shields.io/badge/JDK-17+-4EB1BA.svg)](https://docs.oracle.com/en/java/javase/17/)
[![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x%20%7C%204.x-green.svg)](https://docs.spring.io/spring-boot/docs/current/reference/html/)

A Spring Boot friendly starter for ShardingSphere JDBC on Spring Boot 3/4, with conservative defaults and practical sharding setup.

## Why This Project

1. Fast ShardingSphere JDBC integration for Spring Boot 3/4 after the official starter was discontinued.
2. Safer adoption in existing systems with conservative defaults (explicit enablement + DataSource back-off).
3. Clear configuration path with practical examples for common sharding scenarios.
4. Explicit compatibility scope: Spring Boot 3.x / 4.x with CI verification.
5. JDK 17+ runtime compatibility so it fits most existing production environments.

## Goals

1. Provide a usable Spring Boot option after the official starter was discontinued.
2. Keep configuration focused and practical for common sharding scenarios.
3. Stay non-invasive by default (explicit enable + datasource back-off behavior).

## Compatibility

| starter | shardingsphere-jdbc | Spring Boot (Supported) | CI Verified |
| --- | --- | --- | --- |
| 1.0.4 | 5.5.2 | 3.x | 3.4.x |
| 1.0.4 | 5.5.2 | 4.x | 4.0.x |

## Quick Start

### 1) Dependency

```xml
<dependency>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>shardingsphere-jdbc-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

### 2) Explicit Enable

```yaml
shardingsphere:
  enabled: true
```

This starter does not take over an existing DataSource bean by default.

This is intentional and conservative: if your app already has a regular business DataSource bean, this starter backs off and will not create a sharding DataSource automatically.

### Recommended Usage

1. If you want this starter to create and own the sharding datasource, do not define another regular DataSource bean.
2. If you want full custom datasource wiring, create and inject ShardingSphereDataSource manually.

### 3) Minimal Table Sharding Example

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

### Database Sharding

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

### Database + Table Sharding

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

### CLASS_BASED Algorithm

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

Build strategy:

1. Build with Java 25 toolchain.
2. Use --release 17 so artifacts run on JDK 17+.

Per-version verification:

```bash
# Boot 3
./gradlew clean test -PspringBootVersion=3.4.8

# Boot 4
./gradlew clean test -PspringBootVersion=4.0.0
```

## Release

Release is tag-driven via Gradle + GitHub Actions.

See [docs/release.md](docs/release.md) for full release steps.

Wiki index (CN): [docs/wiki/README.md](docs/wiki/README.md)

Wiki index (EN): [docs/wiki/README_EN.md](docs/wiki/README_EN.md)

## Upgrade Notes (5.5.x)

1. This starter aligns to shardingsphere-jdbc 5.5.2; avoid adding conflicting shardingsphere-jdbc versions in your app.
2. Official Spring Boot Starter shape was removed since 5.3.0; use this starter or switch to the official JDBC Driver approach.
3. During upgrades, re-validate sharding algorithm props and SQL routing behavior.

## Contributing

Issues and PRs are welcome.

If this project helps you, a star is appreciated.

## License

[Apache License 2.0](LICENSE)
