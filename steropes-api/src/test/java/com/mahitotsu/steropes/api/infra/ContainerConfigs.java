package com.mahitotsu.steropes.api.infra;

import org.slf4j.LoggerFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class ContainerConfigs {

    @Bean
    @ServiceConnection
    public RabbitMQContainer rabbitMQContainer() {
        final RabbitMQContainer container = new RabbitMQContainer(DockerImageName
                .parse("public.ecr.aws/docker/library/rabbitmq:latest").asCompatibleSubstituteFor("rabbitmq"));
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(RabbitMQContainer.class)));
        return container;
    }
    
}
