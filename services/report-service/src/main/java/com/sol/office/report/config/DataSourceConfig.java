package com.sol.office.report.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Two connection pools:
 * <ul>
 *   <li>primary ({@code spring.datasource}) - this service's own schema (catalog, history); used by JPA/Flyway;</li>
 *   <li>reporting ({@code office.report.datasource}) - the data the templates query, typically a read replica
 *       of the core system. Kept separate so heavy reports cannot starve the service's own pool.</li>
 * </ul>
 */
@Configuration
public class DataSourceConfig {

    public static final String REPORTING_DATA_SOURCE = "reportingDataSource";

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(@Qualifier("appDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties("office.report.datasource")
    public DataSourceProperties reportingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(REPORTING_DATA_SOURCE)
    @ConfigurationProperties("office.report.datasource.hikari")
    public HikariDataSource reportingDataSource(@Qualifier("reportingDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
