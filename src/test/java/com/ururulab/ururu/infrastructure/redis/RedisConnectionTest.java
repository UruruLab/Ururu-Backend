package com.ururulab.ururu.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Redis Connection Test")
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Redis server connection test")
    void testRedisConnection() {
        // Given
        final String testKey = "test:connection:" + System.currentTimeMillis();
        final String testValue = "Redis connection test at " + LocalDateTime.now();

        // When
        redisTemplate.opsForValue().set(testKey, testValue);
        final String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);

        // Then
        assertThat(retrievedValue).isEqualTo(testValue);

        // Cleanup
        redisTemplate.delete(testKey);
    }

    @Test
    @DisplayName("Redis data types operation test")
    void testRedisDataTypes() {
        final String keyPrefix = "test:types:" + System.currentTimeMillis();

        try {
            // String type test
            redisTemplate.opsForValue().set(keyPrefix + ":string", "test-value");
            assertThat(redisTemplate.opsForValue().get(keyPrefix + ":string")).isEqualTo("test-value");

            // Hash type test
            redisTemplate.opsForHash().put(keyPrefix + ":hash", "field1", "value1");
            assertThat(redisTemplate.opsForHash().get(keyPrefix + ":hash", "field1")).isEqualTo("value1");

            // List type test
            redisTemplate.opsForList().rightPush(keyPrefix + ":list", "item1");
            redisTemplate.opsForList().rightPush(keyPrefix + ":list", "item2");
            assertThat(redisTemplate.opsForList().size(keyPrefix + ":list")).isEqualTo(2);

        } finally {
            // Cleanup
            redisTemplate.delete(keyPrefix + ":string");
            redisTemplate.delete(keyPrefix + ":hash");
            redisTemplate.delete(keyPrefix + ":list");
        }
    }
}
