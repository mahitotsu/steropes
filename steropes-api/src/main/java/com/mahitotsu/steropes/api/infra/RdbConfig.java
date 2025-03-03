package com.mahitotsu.steropes.api.infra;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.NonNull;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dsql.DsqlUtilities;

@Configuration
public class RdbConfig {

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {

        final DataSourceProperties props = new DataSourceProperties();
        props.setType(HikariDataSource.class);

        final ProxyFactory factory = new ProxyFactory();
        factory.setTarget(props);
        factory.addAdvice(new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                final String name = invocation.getMethod().getName();
                switch (name) {
                    case "setType":
                        throw new UnsupportedOperationException("This property cannot be overwritten.");
                    default:
                        return invocation.proceed();
                }
            }
        });

        return DataSourceProperties.class.cast(factory.getProxy());
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {

        final HikariConfig config = new HikariConfig();
        // Setting default values ​​for properties that require values
        config.setDriverClassName(Driver.class.getCanonicalName());
        config.setExceptionOverrideClassName(DsqlExceptionOverride.class.getCanonicalName());
        config.setMaximumPoolSize(16);
        config.setMinimumIdle(0);

        final ProxyFactory factory = new ProxyFactory();
        factory.setTarget(config);
        factory.addAdvice(new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                final String name = invocation.getMethod().getName();
                switch (name) {
                    case "setDriverClassName":
                    case "setExceptionOverrideClassName":
                        throw new UnsupportedOperationException("This property cannot be overwritten.");
                    default:
                        return invocation.proceed();
                }
            }
        });

        return HikariConfig.class.cast(factory.getProxy());
    }

    @Bean
    @Qualifier("targetDataSource")
    public DataSource targetDataSource(@Autowired final AwsCredentialsProvider awsCredentialsProvider)
            throws SQLException {

        final DataSourceProperties dataSourceProperties = dataSourceProperties();
        final PGSimpleDataSource targetDataSource = new PGSimpleDataSource();
        targetDataSource.setURL(dataSourceProperties.getUrl());

        final String endpoint = dataSourceProperties.getUrl().split("/")[2];
        final String username = dataSourceProperties.getUsername();
        final String password = dataSourceProperties.getPassword();
        final Region region = Region.of(endpoint.split("\\.")[2]);
        final DsqlUtilities dsqlUtilities = DsqlUtilities.builder().region(region)
                .credentialsProvider(awsCredentialsProvider).build();

        return new DelegatingDataSource(targetDataSource) {

            @Override
            public @NonNull Connection getConnection() throws SQLException {
                return this.getConnection(username, password);
            }

            @Override
            public @NonNull Connection getConnection(final String username, final String password) throws SQLException {
                final String token = "admin".equals(username)
                        ? dsqlUtilities
                                .generateDbConnectAdminAuthToken(request -> request.hostname(endpoint).region(region))
                        : dsqlUtilities
                                .generateDbConnectAuthToken(request -> request.hostname(endpoint).region(region));
                return targetDataSource.getConnection(username, token);
            }
        };
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("targetDataSource") final DataSource targetDataSource) {

        final DataSourceProperties dataSourceProperties = dataSourceProperties();
        final HikariDataSource hikariDataSource = dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        final HikariConfig hikariConfig = this.hikariConfig();
        BeanUtils.copyProperties(hikariConfig, hikariDataSource);

        hikariDataSource.setDataSource(targetDataSource);
        return hikariDataSource;
    }

    @Bean
    @Profile("drop-create")
    public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {

        final DataSourceInitializer initializer = new DataSourceInitializer();

        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(
                new ResourceDatabasePopulator(this.resourceLoader.getResource("classpath:initdb/schema.sql")));
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
