package com.ururulab.ururu.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private static final Duration RESERVATION_TTL = Duration.ofMinutes(30);
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(5);

    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
            local expired = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
            for _, member in ipairs(expired) do redis.call('HDEL', KEYS[1], member) end
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
            local current = tonumber(redis.call('HGET', KEYS[1], ARGV[2]) or '0')
            local total = 0
            for _, value in ipairs(redis.call('HVALS', KEYS[1])) do total = total + tonumber(value) end
            local requested = tonumber(ARGV[3])
            local available = tonumber(ARGV[4]) - total + current
            if available < requested then return -1 end
            redis.call('HSET', KEYS[1], ARGV[2], requested)
            redis.call('ZADD', KEYS[2], ARGV[5], ARGV[2])
            redis.call('PEXPIRE', KEYS[1], ARGV[6])
            redis.call('PEXPIRE', KEYS[2], ARGV[6])
            return available - requested
            """, Long.class);

    private static final DefaultRedisScript<Long> RESERVED = new DefaultRedisScript<>("""
            local expired = redis.call('ZRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
            for _, member in ipairs(expired) do redis.call('HDEL', KEYS[1], member) end
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[1])
            if ARGV[2] ~= '' then return tonumber(redis.call('HGET', KEYS[1], ARGV[2]) or '0') end
            local total = 0
            for _, value in ipairs(redis.call('HVALS', KEYS[1])) do total = total + tonumber(value) end
            return total
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_RESERVATION = new DefaultRedisScript<>("""
            redis.call('ZREM', KEYS[2], ARGV[1])
            return redis.call('HDEL', KEYS[1], ARGV[1])
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;

    public int reserveStock(Long optionId, Long memberId, int quantity, int totalStock) {
        long now = Instant.now().toEpochMilli();
        Long remaining = redisTemplate.execute(
                RESERVE,
                reservationKeys(optionId),
                Long.toString(now), memberId.toString(), Integer.toString(quantity), Integer.toString(totalStock),
                Long.toString(now + RESERVATION_TTL.toMillis()), Long.toString(RESERVATION_TTL.toMillis())
        );
        return remaining == null ? -1 : remaining.intValue();
    }

    public void releaseReservation(Long optionId, Long memberId) {
        redisTemplate.execute(RELEASE_RESERVATION, reservationKeys(optionId), memberId.toString());
    }

    public int getTotalReservedQuantity(Long optionId) {
        return reservedQuantity(optionId, "");
    }

    public int getReservedQuantity(Long optionId, Long memberId) {
        return reservedQuantity(optionId, memberId.toString());
    }

    public String tryAcquireProcessingLock(Long memberId) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(processingKey(memberId), token, PROCESSING_TTL);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void releaseProcessingLock(Long memberId, String token) {
        redisTemplate.execute(RELEASE_LOCK, List.of(processingKey(memberId)), token);
    }

    private int reservedQuantity(Long optionId, String memberId) {
        Long result = redisTemplate.execute(
                RESERVED, reservationKeys(optionId), Long.toString(Instant.now().toEpochMilli()), memberId);
        return result == null ? 0 : result.intValue();
    }

    private List<String> reservationKeys(Long optionId) {
        String slot = "{" + optionId + "}";
        return List.of("pending_order:" + slot + ":quantities", "pending_order:" + slot + ":expirations");
    }

    private String processingKey(Long memberId) {
        return "processing:" + memberId;
    }
}
