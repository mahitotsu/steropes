package com.mahitotsu.steropes.api.config;

import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@TestConfiguration
public class DataConfig {
    
    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    public DataSourceInitializer dataSourceInitializer(@Autowired final DataSource dataSource) {

        final DataSourceInitializer initializer = new DataSourceInitializer();

        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(
                new ResourceDatabasePopulator(this.resourceLoader.getResource("classpath:schema.sql")));
        initializer.setDatabaseCleaner((con) -> {
            String fetchTablesQuery = "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname = 'public'";
            ResultSet resultSet = con.createStatement().executeQuery(fetchTablesQuery);
            while (resultSet.next()) {
                String tableName = resultSet.getString("tablename");
                String dropTableQuery = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
                con.createStatement().executeUpdate(dropTableQuery);
            }
        });

        return initializer;
    }
}
