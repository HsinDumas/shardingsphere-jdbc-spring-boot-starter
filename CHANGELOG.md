# Changelog

All notable changes to this project are documented in this file.

## [1.1.1] - 2026-07-30

### Fixed

- Bundle the ShardingSphere MySQL SQL parser engine so consumer applications can load `DialectSQLParserFacade` at runtime.
- Register ShardingSphere's native wildcard single-table rule so existing ordinary tables no longer need explicit single-node entries under `shardingsphere.tables`.

### Changed

- Keep `shardingsphere.tables` focused on tables that actually require database or table sharding.
- Align the local and CI build toolchain on Java 21 while retaining Java 17 bytecode compatibility.

### Tests

- Added a prepared-statement regression test for the MySQL parser SPI.
- Added a real insert test for an existing physical table omitted from `shardingsphere.tables`.

See [GitHub issue #2](https://github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter/issues/2).

## [1.1.0] - 2026-07-06

- Upgraded ShardingSphere JDBC to 5.5.3.
- Added Spring Boot 4 compatibility and Java 17 runtime compatibility.
- Added tag-driven Maven Central publishing and GitHub Releases.

[1.1.1]: https://github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter/releases/tag/v1.1.0
