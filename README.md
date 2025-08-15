# shardingsphere-jdbc-spring-boot-starter

[![License](https://img.shields.io/badge/license-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html)
[![License](https://img.shields.io/badge/JDK-17+-4EB1BA.svg)](https://docs.oracle.com/en/java/javase/17/)
[![License](https://img.shields.io/badge/SpringBoot-3+-green.svg)](https://docs.spring.io/spring-boot/docs/2.1.5.RELEASE/reference/htmlsingle/)
[![Author](https://img.shields.io/badge/Author-HsinDumas-orange.svg?style=flat-square)](https://github.com/HsinDumas)

**适用于 shardingsphere-jdbc 的 spring-boot-starter (Spring Boot 3)**。

1. 由于官方提供的 starter 停止维护，因此本项目基于 shardingsphere-jdbc 进行一定的配置封装
2. 目前只适配了 Spring Boot 3.x
3. 从 shardingsphere-jdbc 5.2.2 开始，同步更新 starter 版本，版本号保持一致
4. 使用 hikari 作为数据库连接池

> 🚀项目持续优化迭代，欢迎大家提ISSUE和PR！麻烦大家能给一颗star✨，您的star是我们持续更新的动力！

## 快速开始

### 引入依赖

```xml

<dependency>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>shardingsphere-jdbc-spring-boot-starter</artifactId>
    <version>5.2.2</version>
</dependency>
```

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

具体 props 请看源码 org.apache.shardingsphere.sharding.algorithm.sharding.classbased.ClassBasedShardingAlgorithm)

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