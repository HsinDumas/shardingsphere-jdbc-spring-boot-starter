package com.github.dumas.shardingsphere.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;

class ShardingsphereJdbcAutoConfigurationTest {

    private static final String DEFAULT_JDBC_URL = PlainTableTestSupport.jdbcUrl("testdb");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShardingsphereJdbcAutoConfiguration.class));

    @Test
    void shouldNotCreateDataSourceWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
    }

    @Test
    void shouldCreateDataSourceWhenEnabledAndNoUserBean() {
        contextRunner
                .withPropertyValues(PlainTableTestSupport.commonShardingProperties(DEFAULT_JDBC_URL))
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context).hasBean("dataSource");
                });
    }

    @Test
    void shouldPrepareStatementWithMysqlParser() {
        contextRunner
                .withPropertyValues(PlainTableTestSupport.commonShardingProperties(DEFAULT_JDBC_URL))
                .run(context -> {
                    DataSource dataSource = context.getBean(DataSource.class);
                    try (Connection connection = dataSource.getConnection();
                            PreparedStatement ignored = connection.prepareStatement("SELECT 1")) {
                        assertThat(connection.isClosed()).isFalse();
                    }
                });
    }

    @Test
    void shouldRouteUnconfiguredTableWithSingleRule() throws Exception {
        String jdbcUrl = PlainTableTestSupport.jdbcUrl("plain_table");
        PlainTableTestSupport.createPlainTable(jdbcUrl);

        contextRunner
                .withPropertyValues(PlainTableTestSupport.commonShardingProperties(jdbcUrl))
                .run(context -> {
                    DataSource dataSource = context.getBean(DataSource.class);
                    assertThat(PlainTableTestSupport.insertPlainTableRow(dataSource, 1L)).isEqualTo(1);
                });
    }

    @Test
    void shouldBackOffWhenUserProvidesDataSource() {
        contextRunner
                .withPropertyValues(PlainTableTestSupport.commonShardingProperties(DEFAULT_JDBC_URL))
                .withUserConfiguration(UserDataSourceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context).hasBean("customDataSource");
                    assertThat(context).doesNotHaveBean("dataSource");
                });
    }

    @Configuration
    static class UserDataSourceConfiguration {
        @Bean
        DataSource customDataSource() {
            return new com.zaxxer.hikari.HikariDataSource();
        }
    }
}
