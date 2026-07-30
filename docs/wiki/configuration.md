# Configuration Examples

## Sharding by Table

```yaml
shardingsphere:
  enabled: true
  data-sources:
    - jdbc-url: jdbc:mysql://127.0.0.1:3306/test
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
  tables:
    - logic-table: t_order
      actual-data-nodes: ds_${0..0}.t_order_${0..1}
      table-strategy:
        algorithm-type: inline
        sharding-column: order_id
        props:
          algorithm-expression: t_order_${order_id % 2}
```

## Class-Based Algorithm

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

## Ordinary / Single Tables

Existing physical tables that do not require sharding are discovered automatically. Do not add them to `shardingsphere.tables`; keep that list for tables with actual sharding rules.

```yaml
shardingsphere:
  enabled: true
  data-sources:
    - jdbc-url: jdbc:mysql://127.0.0.1:3306/test
      username: root
      password: root
      driver-class-name: com.mysql.cj.jdbc.Driver
  tables:
    # Only sharded tables belong here. An existing `customer_profile` table is auto-discovered.
    - logic-table: t_order
      actual-data-nodes: ds_0.t_order_${0..1}
      table-strategy:
        algorithm-type: inline
        sharding-column: order_id
        props:
          algorithm-expression: t_order_${order_id % 2}
```

The physical table must already exist when the starter initializes so ShardingSphere can load its metadata.
