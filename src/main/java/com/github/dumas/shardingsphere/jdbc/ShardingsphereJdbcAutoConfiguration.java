package com.github.dumas.shardingsphere.jdbc;

import com.github.dumas.shardingsphere.jdbc.properties.ShardingProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.mode.repository.standalone.StandalonePersistRepositoryConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Dumas
 */
@AutoConfiguration
@EnableConfigurationProperties(ShardingProperties.class)
public class ShardingsphereJdbcAutoConfiguration {

    private final ShardingProperties shardingProperties;

    public ShardingsphereJdbcAutoConfiguration(ShardingProperties shardingProperties) {
        this.shardingProperties = shardingProperties;
    }

    @Bean
    public DataSource dataSource() throws SQLException {

        Map<String, DataSource> dataSourceMap = new HashMap<>();
        for (int i = 0; i < shardingProperties.getDataSources().size(); i++) {
            dataSourceMap.put(
                    "ds_" + i, dataSource(shardingProperties.getDataSources().get(i)));
        }

        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        for (ShardingProperties.Table table : shardingProperties.getTables()) {

            ShardingTableRuleConfiguration tableRuleConfiguration =
                    new ShardingTableRuleConfiguration(table.getLogicTable(), table.getActualDataNodes());

            Optional.ofNullable(table.getTableStrategy()).ifPresent(o -> {
                String algorithmName = table.getLogicTable() + "_table";
                tableRuleConfiguration.setTableShardingStrategy(
                        new StandardShardingStrategyConfiguration(o.getShardingColumn(), algorithmName));
                AlgorithmConfiguration algorithmConfiguration =
                        new AlgorithmConfiguration(o.getAlgorithmType().name(), o.getProps());
                shardingRuleConfig.getShardingAlgorithms().put(algorithmName, algorithmConfiguration);
            });

            Optional.ofNullable(table.getDatabaseStrategy()).ifPresent(o -> {
                String algorithmName = table.getLogicTable() + "_db";
                tableRuleConfiguration.setDatabaseShardingStrategy(
                        new StandardShardingStrategyConfiguration(o.getShardingColumn(), algorithmName));
                AlgorithmConfiguration algorithmConfiguration =
                        new AlgorithmConfiguration(o.getAlgorithmType().name(), o.getProps());
                shardingRuleConfig.getShardingAlgorithms().put(algorithmName, algorithmConfiguration);
            });

            shardingRuleConfig.getTables().add(tableRuleConfiguration);
        }
        return ShardingSphereDataSourceFactory.createDataSource(
                new ModeConfiguration(
                        "Standalone", new StandalonePersistRepositoryConfiguration("JDBC", new Properties())),
                dataSourceMap,
                List.of(shardingRuleConfig),
                shardingProperties.getProps());
    }

    private HikariDataSource dataSource(ShardingProperties.DataSource dd) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dd.getJdbcUrl());
        dataSource.setUsername(dd.getUsername());
        dataSource.setPassword(dd.getPassword());
        dataSource.setDriverClassName(dd.getDriverClassName());
        dataSource.setMaximumPoolSize(dd.getMaxPoolSize());
        dataSource.setMinimumIdle(dd.getMinIdle());
        dataSource.setIdleTimeout(dd.getIdleTimeout());
        if (!StringUtils.hasLength(dd.getConnectionTestQuery())) {
            dataSource.setConnectionTestQuery(dd.getConnectionTestQuery());
        }
        return dataSource;
    }
}
