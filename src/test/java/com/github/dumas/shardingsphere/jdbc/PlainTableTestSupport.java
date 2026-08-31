package com.github.dumas.shardingsphere.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

final class PlainTableTestSupport {
    private PlainTableTestSupport() {
    }

    static String jdbcUrl(final String databaseName) {
        if (!databaseName.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe H2 database name: " + databaseName);
        }
        return "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;USER=sa;PASSWORD=sa";
    }

    static void createPlainTable(final String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE plain_table (id BIGINT PRIMARY KEY)");
        }
    }

    static int insertPlainTableRow(final DataSource dataSource, final long id) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO plain_table (id) VALUES (?)")) {
            statement.setLong(1, id);
            return statement.executeUpdate();
        }
    }

    static int countPlainTableRows(final String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "sa");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM plain_table")) {
            if (!result.next()) {
                throw new IllegalStateException("plain_table count query returned no row");
            }
            return result.getInt(1);
        }
    }

    static String[] commonShardingProperties(final String jdbcUrl) {
        return new String[] {
                "shardingsphere.enabled=true",
                "shardingsphere.data-sources[0].jdbc-url=" + jdbcUrl,
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
}
