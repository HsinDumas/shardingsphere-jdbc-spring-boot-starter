package com.github.dumas.shardingsphere.jdbc;

import io.github.ym0506.routecontract.CapturedResult;
import io.github.ym0506.routecontract.RouteAssertions;
import io.github.ym0506.routecontract.RouteContract;
import io.github.ym0506.routecontract.RouteSnapshot;
import io.github.ym0506.routecontract.manifest.DataSourceAliases;
import io.github.ym0506.routecontract.manifest.ManifestPolicy;
import io.github.ym0506.routecontract.manifest.ManifestAssertions;
import io.github.ym0506.routecontract.manifest.ManifestStore;
import io.github.ym0506.routecontract.manifest.ManifestVerifier;
import io.github.ym0506.routecontract.manifest.ObservedExecutionManifest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.JarURLConnection;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.shardingsphere.infra.executor.sql.hook.SQLExecutionHook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class RouteContractPlainTablePilotTest {
    private static final String JDBC_URL =
            "jdbc:h2:mem:routecontract_plain_table;MODE=MySQL;DB_CLOSE_DELAY=-1;USER=sa;PASSWORD=sa";
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShardingsphereJdbcAutoConfiguration.class));

    @Test
    void writesCandidateThenStopsForHumanBaselineApproval() throws Exception {
        Path loadedJar = Path.of(RouteContract.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        // Rehash the actual CodeSource in this JVM immediately before the observed operation.
        String loadedSha = java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(loadedJar)));
        assertThat(loadedJar.getFileName().toString()).isEqualTo("routecontract-shardingsphere-5.5-0.1.2.jar");
        assertThat(loadedSha).isEqualTo(System.getProperty("routecontract.expectedSha256"));
        SQLExecutionHook provider = ServiceLoader.load(SQLExecutionHook.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(hook -> hook.getClass().getName().equals(
                        "io.github.ym0506.routecontract.internal.RouteContractSqlExecutionHook"))
                .findFirst().orElseThrow();
        Path providerJar = Path.of(provider.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        assertThat(providerJar).isEqualTo(loadedJar);
        String descriptor = "META-INF/services/" + SQLExecutionHook.class.getName();
        var descriptorUrls = java.util.Collections.list(RouteContract.class.getClassLoader().getResources(descriptor));
        assertThat(descriptorUrls).anySatisfy(url -> {
            assertThat(url.getProtocol()).isEqualTo("jar");
            try {
                Path descriptorJar = Path.of(((JarURLConnection) url.openConnection())
                        .getJarFileURL().toURI()).toRealPath();
                assertThat(descriptorJar).isEqualTo(loadedJar);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        String preOperationSha = sha256(loadedJar);

        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "sa");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE plain_table (id BIGINT PRIMARY KEY)");
        }
        contextRunner.withPropertyValues(commonShardingProperties())
                .withPropertyValues("shardingsphere.data-sources[0].jdbc-url=" + JDBC_URL)
                .run(context -> {
                    DataSource dataSource = context.getBean(DataSource.class);
                    CapturedResult<Integer> captured = RouteContract.captureResult("plain-table.insert", () -> {
                        try (Connection connection = dataSource.getConnection();
                                PreparedStatement statement = connection.prepareStatement(
                                        "INSERT INTO plain_table (id) VALUES (?)")) {
                            statement.setLong(1, 1L);
                            return statement.executeUpdate();
                        }
                    });
                    assertThat(captured.value()).isEqualTo(1);
                    try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "sa");
                            Statement statement = connection.createStatement();
                            ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM plain_table")) {
                        assertThat(result.next()).isTrue();
                        assertThat(result.getInt(1)).isEqualTo(1);
                    }
                    RouteSnapshot snapshot = captured.snapshot();
                    assertThat(sha256(loadedJar)).isEqualTo(preOperationSha)
                            .isEqualTo(System.getProperty("routecontract.expectedSha256"));
                    RouteAssertions.assertThat(snapshot).hasCompleteCapture().hasNoReportedExecutionFailures()
                            .hasExactlyObservedPhysicalAttempts(1).observesExactlyDataSourceNames("ds_0");
                    ObservedExecutionManifest candidate = ObservedExecutionManifest.from(snapshot,
                            DataSourceAliases.of(Map.of("ds_0", "single-rule-primary")), ManifestPolicy.strict(1, 1));
                    Path projectDir = Path.of(System.getProperty("routecontract.projectDir"));
                    Path approved = projectDir.resolve(
                            "src/routeContractPilot/resources/route-contracts/plain-table.insert.json");
                    Path candidatePath = projectDir.resolve("build/routecontract/plain-table.insert.candidate.json");
                    new ManifestStore().writeCandidate(approved, candidatePath, candidate);
                    if (Files.notExists(approved)) {
                        fail("No human-approved baseline. Review build/routecontract/plain-table.insert.candidate.json");
                    }
                    byte[] approvedBefore = Files.readAllBytes(approved);
                    ManifestAssertions.assertMatched(
                            new ManifestVerifier().verify(new ManifestStore().read(approved), candidate));
                    assertThat(Files.readAllBytes(approved)).isEqualTo(approvedBefore);
                });
    }

    private static String sha256(final Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String[] commonShardingProperties() {
        return new String[] {
                "shardingsphere.enabled=true", "shardingsphere.data-sources[0].jdbc-url=" + JDBC_URL,
                "shardingsphere.data-sources[0].username=sa", "shardingsphere.data-sources[0].password=sa",
                "shardingsphere.data-sources[0].driver-class-name=org.h2.Driver",
                "shardingsphere.tables[0].logic-table=t_order",
                "shardingsphere.tables[0].actual-data-nodes=ds_${0..0}.t_order_${0..1}",
                "shardingsphere.tables[0].table-strategy.algorithm-type=inline",
                "shardingsphere.tables[0].table-strategy.sharding-column=order_id",
                "shardingsphere.tables[0].table-strategy.props.algorithm-expression=t_order_${order_id % 2}"
        };
    }
}
