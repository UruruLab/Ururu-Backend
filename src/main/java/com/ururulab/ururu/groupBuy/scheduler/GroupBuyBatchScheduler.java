package com.ururulab.ururu.groupBuy.scheduler;

import com.ururulab.ururu.groupBuy.service.GroupBuyBatchCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuyBatchScheduler {

    private final GroupBuyBatchCloseService batchCloseService;
    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_KEY = "batch:groupbuy:close";
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) end
            return 0
            """, Long.class);

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void closeExpiredGroupBuys() {
        log.info("Starting hourly batch process for expired group buys");

        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, Duration.ofMinutes(50)))) {
            log.info("Skipping group buy closure; another instance holds the lock");
            return;
        }

        try {
            batchCloseService.closeExpiredGroupBuys();
        } catch (Exception e) {
            log.error("Failed to execute group buy closure batch", e);
        } finally {
            redisTemplate.execute(RELEASE_LOCK, List.of(LOCK_KEY), token);
        }
    }
}
