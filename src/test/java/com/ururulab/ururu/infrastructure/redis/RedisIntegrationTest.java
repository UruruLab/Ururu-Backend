package com.ururulab.ururu.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Redis Integration Test")
class RedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Redis TTL test")
    void testRedisTTL() throws InterruptedException {
        // Given
        final String key = "ttl:test:" + System.currentTimeMillis();
        final String value = "ttl-test-value";
        final Duration ttl = Duration.ofSeconds(2);

        // When
        redisTemplate.opsForValue().set(key, value, ttl);
        
        // Then - immediately after setting
        assertThat(redisTemplate.hasKey(key)).isTrue();
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(value);

        // When - wait for TTL expiration
        Thread.sleep(ttl.toMillis() + 500);

        // Then - after TTL expiration
        assertThat(redisTemplate.hasKey(key)).isFalse();
        assertThat(redisTemplate.opsForValue().get(key)).isNull();
    }

    @Test
    @DisplayName("Redis performance test")
    void testRedisPerformance() {
        // Given
        final String keyPrefix = "perf:test:" + System.currentTimeMillis();
        final int dataCount = 100;

        // When - write performance test
        final long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < dataCount; i++) {
            final String key = keyPrefix + ":" + i;
            final String value = "test-data-" + i;
            redisTemplate.opsForValue().set(key, value);
        }
        
        final long writeTime = System.currentTimeMillis() - startTime;

        // When - read performance test
        final long readStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < dataCount; i++) {
            final String key = keyPrefix + ":" + i;
            redisTemplate.opsForValue().get(key);
        }
        
        final long readTime = System.currentTimeMillis() - readStartTime;

        // Then
        assertThat(writeTime).isLessThan(2000); // 2 seconds
        assertThat(readTime).isLessThan(1000);  // 1 second

        // Cleanup
        for (int i = 0; i < dataCount; i++) {
            final String key = keyPrefix + ":" + i;
            redisTemplate.delete(key);
        }
    }
}
