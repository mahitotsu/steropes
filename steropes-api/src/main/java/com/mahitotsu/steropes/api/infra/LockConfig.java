package com.mahitotsu.steropes.api.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class LockConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.create();
    }

    @Bean
    public AmazonDynamoDBLockClient amazonDynamoDBLockClient() {
        return new AmazonDynamoDBLockClient(
                AmazonDynamoDBLockClientOptions.builder(this.dynamoDbClient(), "lockTable")
                        .build());
    }

    @Bean
    public AmazonDynamoDBLockRegistry amazonDynamoDBLockRegistry() {
        return new AmazonDynamoDBLockRegistry(this.amazonDynamoDBLockClient());
    }
}
