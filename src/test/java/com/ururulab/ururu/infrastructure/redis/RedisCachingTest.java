package com.ururulab.ururu.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Redis Caching Test")
class RedisCachingTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("CacheManager bean existence test")
    void testCacheManagerExists() {
        assertThat(cacheManager).isNotNull();
        // getCacheNames()는 빈 컬렉션일 수 있으므로 null 체크만 진행
    }

    @Test
    @DisplayName("Manual cache operation test")
    void testManualCacheOperation() {
        // Given
        final String cacheName = "testCache";
        final String key = "testKey";
        final String value = "testValue";

        // When - 캐시가 없으면 자동 생성됨
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).put(key, value);
            final String cachedValue = cacheManager.getCache(cacheName).get(key, String.class);

            // Then
            assertThat(cachedValue).isEqualTo(value);

            // Cleanup
            cacheManager.getCache(cacheName).evict(key);
        } else {
            // 캐시가 없는 경우 Redis 직접 테스트
            redisTemplate.opsForValue().set("cache:" + key, value);
            final String directValue = (String) redisTemplate.opsForValue().get("cache:" + key);
            assertThat(directValue).isEqualTo(value);
            redisTemplate.delete("cache:" + key);
        }
    }
}
