package com.github.dumas.shardingsphere.jdbc.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Properties;

/**
 * @author Dumas
 */
@Data
@Validated
@ConfigurationProperties(prefix = "shardingsphere")
public class ShardingProperties {

    /**
     * Whether to enable this starter auto-configuration.
     */
    private boolean enabled;

    @NotEmpty
    private List<@Valid DataSource> dataSources;

    @NotEmpty
    private List<@Valid Table> tables;

    private Properties props;

    public enum AlgorithmType {
        INLINE,
        CLASS_BASED
    }

    @Data
    public static class DataSource {
        @NotBlank
        private String jdbcUrl;

        @NotBlank
        private String username;

        @NotBlank
        private String password;

        @NotBlank
        private String driverClassName;

        private Integer maxPoolSize;

        private Long idleTimeout;

        private Integer minIdle;

        private String connectionTestQuery;

        /**
         * Additional HikariCP-specific settings.
         */
        private Properties hikari;
    }

    @Data
    public static class Table {
        @NotBlank
        private String logicTable;

        @NotBlank
        private String actualDataNodes;

        private @Valid Strategy tableStrategy;

        private @Valid Strategy databaseStrategy;
    }

    @Data
    public static class Strategy {
        @NotNull
        private AlgorithmType algorithmType;

        @NotBlank
        private String shardingColumn;

        @NotNull
        private Properties props;
    }
}
