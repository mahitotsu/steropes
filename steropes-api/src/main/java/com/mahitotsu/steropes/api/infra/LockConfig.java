package com.mahitotsu.steropes.api.infra;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class LockConfig {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    private String buildLockOnwerName() {

        final String hostname = hostname();
        final long pid = ProcessHandle.current().pid();
        final long threadId = Thread.currentThread().threadId();
        return String.format("%s-%016x-%016X", hostname, pid, threadId);
    }

    private String hostname() {

        try {
            return Inet4Address.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            final String hostname = System.getenv("HOSTNAME");
            return hostname != null ? hostname : UUID.randomUUID().toString();
        }
    }

    @Bean
    @Scope("threadlocal")
    public AmazonDynamoDBLockClient amazonDynamoDBLockClient() {

        final AmazonDynamoDBLockClientOptions options = AmazonDynamoDBLockClientOptions
                .builder(this.dynamoDbClient, "lock_table")
                .withPartitionKeyName("p_key")
                .withSortKeyName("s_key")
                .withCreateHeartbeatBackgroundThread(false)
                .withLeaseDuration(30L)
                .withHeartbeatPeriod(Long.MAX_VALUE)
                .withTimeUnit(TimeUnit.SECONDS)
                .withOwnerName(this.buildLockOnwerName())
                .build();
        return new AmazonDynamoDBLockClient(options);
    }
}
