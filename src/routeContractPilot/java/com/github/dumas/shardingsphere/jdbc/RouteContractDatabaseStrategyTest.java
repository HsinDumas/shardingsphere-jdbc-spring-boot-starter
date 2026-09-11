package com.github.dumas.shardingsphere.jdbc;

import io.github.ym0506.routecontract.RouteAssertions;
import io.github.ym0506.routecontract.RouteContract;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteContractDatabaseStrategyTest {
    @Test
    void appliesConfiguredDatabaseStrategyWithoutBroadcasting() throws Exception {
        List<String> properties = new ArrayList<>(List.of(
                "shardingsphere.enabled=true",
                "shardingsphere.tables[0].logic-table=t_order",
                "shardingsphere.tables[0].actual-data-nodes=ds_${0..1}.t_order",
                "shardingsphere.tables[0].database-strategy.algorithm-type=inline",
                "shardingsphere.tables[0].database-strategy.sharding-column=id",
                "shardingsphere.tables[0].database-strategy.props.algorithm-expression=ds_${id % 2}"));
        // Identical synthetic rows make LIMIT 1 return the same result even after accidental broadcast.
        for (int i = 0; i < 2; i++) {
            String jdbcUrl = "jdbc:h2:mem:rc_strategy_" + i + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
            try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE t_order (id BIGINT PRIMARY KEY)");
                statement.execute("INSERT INTO t_order (id) VALUES (1)");
            }
            String prefix = "shardingsphere.data-sources[" + i + "].";
            properties.addAll(List.of(prefix + "jdbc-url=" + jdbcUrl, prefix + "username=sa",
                    prefix + "password=", prefix + "driver-class-name=org.h2.Driver"));
        }
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ShardingsphereJdbcAutoConfiguration.class))
                .withPropertyValues(properties.toArray(String[]::new))
                .run(context -> {
                    var captured = RouteContract.captureResult("database-strategy.select",
                            () -> selectOne(context.getBean(DataSource.class)));
                    assertThat(captured.value()).containsExactly(1L);
                    System.out.printf("ROUTECONTRACT result=%s observedAttempts=%d%n",
                            captured.value(), captured.snapshot().observedPhysicalAttemptCount());
                    RouteAssertions.assertThat(captured.snapshot())
                            .hasCompleteCapture().hasNoReportedExecutionFailures()
                            .hasExactlyObservedPhysicalAttempts(1);
                });
    }

    private static List<Long> selectOne(final DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM t_order WHERE id = ? ORDER BY id LIMIT 1")) {
            statement.setLong(1, 1L);
            try (ResultSet result = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (result.next()) {
                    ids.add(result.getLong(1));
                }
                return ids;
            }
        }
    }
}
