
package com.university.warehouse_etl.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

    // --- CONFIGURATION FOR THE OPERATIONAL DATABASE (SOURCE) ---

    //  Create a bean that holds the properties for the operational DB
    @Bean(name = "operationalDataSourceProperties") 
    @ConfigurationProperties("app.datasource.operational")
    public DataSourceProperties operationalDataSourceProperties() {
        return new DataSourceProperties();
    }

    //  Create the actual data source, explicitly asking for the properties bean above
    @Bean(name = "operationalDataSource")
    @ConfigurationProperties("app.datasource.operational.hikari")
    public HikariDataSource operationalDataSource(
            @Qualifier("operationalDataSourceProperties") DataSourceProperties properties) { 
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    // Create the JDBC template for the operational DB
    @Bean(name = "operationalJdbcTemplate")
    public JdbcTemplate operationalJdbcTemplate(@Qualifier("operationalDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }


    // --- CONFIGURATION FOR THE WAREHOUSE DATABASE (TARGET) ---

    //  Create a bean that holds the properties for the warehouse DB
    @Bean(name = "warehouseDataSourceProperties") 
    @Primary
    @ConfigurationProperties("app.datasource.warehouse")
    public DataSourceProperties warehouseDataSourceProperties() {
        return new DataSourceProperties();
    }

    // Create the actual data source, explicitly asking for the properties bean above
    @Bean(name = "warehouseDataSource")
    @Primary
    @ConfigurationProperties("app.datasource.warehouse.hikari")
    public HikariDataSource warehouseDataSource(
            @Qualifier("warehouseDataSourceProperties") DataSourceProperties properties) { 
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    // Create the JDBC template for the warehouse DB
    @Bean(name = "warehouseJdbcTemplate")
    public JdbcTemplate warehouseJdbcTemplate(@Qualifier("warehouseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}