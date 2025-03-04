package com.mahitotsu.steropes.api.infra;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
                AmazonDynamoDBLockClientOptions.builder(this.dynamoDbClient(), "lock_table")
                        .withPartitionKeyName("pKey")
                        .withSortKeyName("sKey")
                        .withTimeUnit(TimeUnit.SECONDS)
                        .withLeaseDuration(10L)
                        .withHeartbeatPeriod(3L)
                        .withCreateHeartbeatBackgroundThread(true)
                        .build());
    }

    @Bean
    public AmazonDynamoDBLockRegistry amazonDynamoDBLockRegistry() {
        return new AmazonDynamoDBLockRegistry(this.amazonDynamoDBLockClient());
    }

    @Bean
    public LockTemplate lockTemplate() {

        final LockTemplate lockTemplate = new LockTemplate(this.amazonDynamoDBLockRegistry());
        lockTemplate.setLockTimeout(Duration.ofSeconds(10L));
        return lockTemplate;
    }
}
