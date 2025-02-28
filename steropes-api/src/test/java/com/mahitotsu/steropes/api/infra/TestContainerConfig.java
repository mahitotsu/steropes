package com.mahitotsu.steropes.api.infra;

import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@Configuration
public class TestContainerConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgreSQLContainer() {

        final PostgreSQLContainer<?> container = new PostgreSQLContainer<>(
                DockerImageName.parse("postgres:16.6")
                        .asCompatibleSubstituteFor("postgres"));
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(PostgreSQLContainer.class)));
        return container;
    }

    @Bean
    @ServiceConnection
    public RedisContainer redisContainer() {

        final RedisContainer container = new RedisContainer(
                DockerImageName.parse("valkey/valkey:8.1").asCompatibleSubstituteFor("redis"));
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RedisContainer.class)));
        return container;
    }
}
