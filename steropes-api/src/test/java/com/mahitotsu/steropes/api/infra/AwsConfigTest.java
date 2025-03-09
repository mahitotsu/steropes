package com.mahitotsu.steropes.api.infra;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mahitotsu.steropes.api.TestMain;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class AwsConfigTest extends TestMain {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Test
    public void testDynamoDBClient() {
        assertNotNull(this.dynamoDbClient);
    }
}
