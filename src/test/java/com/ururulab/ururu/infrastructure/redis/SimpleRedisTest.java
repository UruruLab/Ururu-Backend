package com.ururulab.ururu.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class SimpleRedisTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void simpleRedisConnectionTest() {
        // Given
        String key = "simple:test:" + System.currentTimeMillis();
        String value = "test-value";

        // When
        redisTemplate.opsForValue().set(key, value);
        String result = (String) redisTemplate.opsForValue().get(key);

        // Then
        assertThat(result).isEqualTo(value);

        // Cleanup
        redisTemplate.delete(key);
    }
}
