package com.github.dumas.shardingsphere.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class ShardingsphereJdbcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShardingsphereJdbcAutoConfiguration.class));

    @Test
    void shouldNotCreateDataSourceWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
    }

    @Test
    void shouldCreateDataSourceWhenEnabledAndNoUserBean() {
        contextRunner
                .withPropertyValues(commonShardingProperties())
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context).hasBean("dataSource");
                });
    }

    @Test
    void shouldBackOffWhenUserProvidesDataSource() {
        contextRunner
                .withPropertyValues(commonShardingProperties())
                .withUserConfiguration(UserDataSourceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context).hasBean("customDataSource");
                    assertThat(context).doesNotHaveBean("dataSource");
                });
    }

    private String[] commonShardingProperties() {
        return new String[] {
                "shardingsphere.enabled=true",
                "shardingsphere.data-sources[0].jdbc-url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;USER=sa;PASSWORD=sa",
                "shardingsphere.data-sources[0].username=sa",
                "shardingsphere.data-sources[0].password=sa",
                "shardingsphere.data-sources[0].driver-class-name=org.h2.Driver",
                "shardingsphere.tables[0].logic-table=t_order",
                "shardingsphere.tables[0].actual-data-nodes=ds_${0..0}.t_order_${0..1}",
                "shardingsphere.tables[0].table-strategy.algorithm-type=inline",
                "shardingsphere.tables[0].table-strategy.sharding-column=order_id",
                "shardingsphere.tables[0].table-strategy.props.algorithm-expression=t_order_${order_id % 2}"
        };
    }

    @Configuration
    static class UserDataSourceConfiguration {
        @Bean
        DataSource customDataSource() {
            return new com.zaxxer.hikari.HikariDataSource();
        }
    }
}
