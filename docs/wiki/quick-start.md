# Quick Start

## Dependency

```xml
<dependency>
    <groupId>com.github.hsindumas</groupId>
    <artifactId>shardingsphere-jdbc-spring-boot-starter</artifactId>
  <version>1.1.1</version>
</dependency>
```

## Enable Starter

```yaml
shardingsphere:
  enabled: true
```

## Conservative Default

If your application already defines a regular `DataSource` bean, this starter backs off and does not create a sharding `DataSource` automatically.

## Ordinary Tables

Since 1.1.1, existing ordinary physical tables are discovered automatically. Only tables that actually require sharding belong under `shardingsphere.tables`. Physical tables must exist before the starter initializes.
