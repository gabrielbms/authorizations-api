package dev.bueno.authorizations.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import static org.junit.jupiter.api.Assertions.*;

class DynamoDbConfigTest {

    private DynamoDbConfig config;

    @BeforeEach
    void setUp() {
        config = new DynamoDbConfig();

        ReflectionTestUtils.setField(config, "endpoint", "http://localhost:8000");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKey", "test");
        ReflectionTestUtils.setField(config, "secretKey", "test");
    }

    @Test
    void shouldCreateDynamoDbClient() {
        DynamoDbClient client = config.dynamoDbClient();
        assertNotNull(client);
    }

    @Test
    void shouldCreateEnhancedClient() {
        DynamoDbClient dynamoDbClient = config.dynamoDbClient();
        DynamoDbEnhancedClient enhancedClient = config.dynamoDbEnhancedClient(dynamoDbClient);
        assertNotNull(enhancedClient);
    }
}