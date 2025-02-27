package com.mahitotsu.steropes.api.infra;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

@Configuration
public class InfraConfig {

    @Bean
    public Sender sernder(final CachingConnectionFactory cf) {
        return RabbitFlux.createSender(new SenderOptions().connectionFactory(cf.getRabbitConnectionFactory()));
    }

    public Receiver receiver(final CachingConnectionFactory cf) {
        return RabbitFlux.createReceiver(new ReceiverOptions().connectionFactory(cf.getRabbitConnectionFactory()));
    }
}
