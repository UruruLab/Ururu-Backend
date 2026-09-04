package com.ururulab.ururu.auth.storage;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public final class RefreshTokenStorage {

    private static final DefaultRedisScript<Long> STORE_SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local token = ARGV[1]
            local expiresAt = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local ttl = tonumber(ARGV[4])
            local maxTokens = tonumber(ARGV[5])
            redis.call('ZREMRANGEBYSCORE', key, '-inf', now)
            redis.call('ZADD', key, expiresAt, token)
            local count = redis.call('ZCARD', key)
            if count > maxTokens then
                redis.call('ZPOPMIN', key, count - maxTokens)
            end
            redis.call('EXPIRE', key, ttl)
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not score then return 0 end
            if tonumber(score) <= tonumber(ARGV[2]) then
                redis.call('ZREM', KEYS[1], ARGV[1])
                return 0
            end
            return redis.call('ZREM', KEYS[1], ARGV[1])
            """, Long.class);

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not score then return 0 end
            if tonumber(score) <= tonumber(ARGV[3]) then
                redis.call('ZREM', KEYS[1], ARGV[1])
                return 0
            end
            redis.call('ZREM', KEYS[1], ARGV[1])
            redis.call('ZADD', KEYS[1], ARGV[4], ARGV[2])
            redis.call('EXPIRE', KEYS[1], ARGV[5])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> CONSUME_LEGACY_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            if not stored or stored ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void storeRefreshToken(final Long userId, final String userType, final String refreshToken) {
        migrateLegacyTokens(userType, userId);

        final long now = Instant.now().toEpochMilli();
        final long expirySeconds = jwtTokenProvider.getRefreshTokenExpirySeconds();
        redisTemplate.execute(
                STORE_SCRIPT,
                List.of(buildRefreshKey(userType, userId)),
                tokenId(refreshToken),
                String.valueOf(now + expirySeconds * 1000),
                String.valueOf(now),
                String.valueOf(expirySeconds),
                String.valueOf(AuthConstants.MAX_REFRESH_TOKENS_PER_USER)
        );
    }

    /** Refresh Token Rotation을 위해 토큰을 한 번만 원자적으로 소비합니다. */
    public boolean consumeRefreshToken(final String userType, final Long userId, final String refreshToken) {
        final Long consumed = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(buildRefreshKey(userType, userId)),
                tokenId(refreshToken),
                String.valueOf(Instant.now().toEpochMilli())
        );
        if (Long.valueOf(1).equals(consumed)) {
            return true;
        }

        final String legacyKey = buildLegacyRefreshKey(
                userType,
                userId,
                jwtTokenProvider.getTokenId(refreshToken)
        );
        return Long.valueOf(1).equals(redisTemplate.execute(
                CONSUME_LEGACY_SCRIPT,
                List.of(legacyKey),
                refreshToken
        ));
    }

    public boolean rotateRefreshToken(
            final String userType,
            final Long userId,
            final String currentToken,
            final String newToken) {
        final long now = Instant.now().toEpochMilli();
        final long expirySeconds = jwtTokenProvider.getRefreshTokenExpirySeconds();
        final Long rotated = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(buildRefreshKey(userType, userId)),
                tokenId(currentToken),
                tokenId(newToken),
                String.valueOf(now),
                String.valueOf(now + expirySeconds * 1000),
                String.valueOf(expirySeconds)
        );
        if (Long.valueOf(1).equals(rotated)) {
            return true;
        }

        if (!consumeRefreshToken(userType, userId, currentToken)) {
            return false;
        }
        storeRefreshToken(userId, userType, newToken);
        return true;
    }

    public void deleteAllRefreshTokens(final String userType, final Long userId) {
        final List<String> keys = new ArrayList<>();
        keys.add(buildRefreshKey(userType, userId));
        keys.addAll(findLegacyKeys(userType, userId));
        redisTemplate.delete(keys);
    }

    private void migrateLegacyTokens(final String userType, final Long userId) {
        final long now = Instant.now().toEpochMilli();
        final String refreshKey = buildRefreshKey(userType, userId);

        for (final String legacyKey : findLegacyKeys(userType, userId)) {
            final String token = redisTemplate.opsForValue().get(legacyKey);
            final Long ttl = redisTemplate.getExpire(legacyKey);
            if (token != null && ttl != null && ttl > 0) {
                redisTemplate.opsForZSet().add(refreshKey, tokenId(token), now + ttl * 1000);
            }
            redisTemplate.delete(legacyKey);
        }
    }

    private List<String> findLegacyKeys(final String userType, final Long userId) {
        final ScanOptions options = ScanOptions.scanOptions()
                .match(buildRefreshKey(userType, userId) + ":*")
                .count(AuthConstants.MAX_REFRESH_TOKENS_PER_USER + 1L)
                .build();
        final List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }

    private String buildRefreshKey(final String userType, final Long userId) {
        final String prefix = UserType.fromString(userType) == UserType.MEMBER
                ? AuthConstants.REFRESH_MEMBER_KEY_PREFIX
                : AuthConstants.REFRESH_SELLER_KEY_PREFIX;
        return prefix + userId;
    }

    private String tokenId(final String token) {
        return jwtTokenProvider.getTokenId(token);
    }

    private String buildLegacyRefreshKey(final String userType, final Long userId, final String tokenId) {
        return buildRefreshKey(userType, userId) + ":" + tokenId;
    }
}
