package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRankingService {

    private final OrderItemRepository orderItemRepository;
    private final GroupBuyRepository groupBuyRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis 키 상수
    private static final String RANKING_KEY = "groupbuy:ranking";
    private static final String ORDER_COUNT_PREFIX = "groupbuy:order_count:";

    /**
     * 여러 공동구매의 주문량 조회 (Redis 배치 조회)
     */
    public Map<Long, Integer> getOrderCounts(List<Long> groupBuyIds) {
        try {
            // 1. Redis 키 목록 생성
            List<String> keys = groupBuyIds.stream()
                    .map(id -> ORDER_COUNT_PREFIX + id)
                    .toList();

            // 2. 배치로 한번에 조회 (네트워크 요청 1번)
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);

            // 3. 결과 매핑
            Map<Long, Integer> result = new HashMap<>();
            for (int i = 0; i < groupBuyIds.size(); i++) {
                String value = values.get(i);
                Integer count = value != null ? Integer.valueOf(value) : 0;
                result.put(groupBuyIds.get(i), count);
            }

            log.debug("Retrieved order counts for {} group buys from Redis", groupBuyIds.size());
            return result;

        } catch (Exception e) {
            log.error("Failed to get order counts from Redis, falling back to DB", e);
            // Redis 실패 시 DB로 폴백
            return getOrderCountsFromDB(groupBuyIds);
        }
    }

    /**
     * DB에서 주문량 조회 (폴백용)
     */
    private Map<Long, Integer> getOrderCountsFromDB(List<Long> groupBuyIds) {
        List<Object[]> results = orderItemRepository.getTotalQuantitiesByGroupBuyIds(groupBuyIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));
    }

    /**
     * DB와 Redis 동기화 (애플리케이션 시작 시)
     */
    @PostConstruct
    public void initializeRedisData() {
        log.info("Initializing Redis data from database...");
        syncOrderCountsFromDB();
    }

    public void syncOrderCountsFromDB() {
        try {
            // 모든 공동구매 ID 조회
            List<Long> allGroupBuyIds = groupBuyRepository.findAllPublic()
                    .stream()
                    .map(GroupBuy::getId)
                    .collect(Collectors.toList());

            if (allGroupBuyIds.isEmpty()) {
                log.info("No group buys found for synchronization");
                return;
            }

            // DB에서 주문량 조회
            List<Object[]> results = orderItemRepository.getTotalQuantitiesByGroupBuyIds(allGroupBuyIds);

            // Redis 데이터 초기화
            stringRedisTemplate.delete(RANKING_KEY);
            Set<String> keysToDelete = new HashSet<>();
            stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
                Cursor<byte[]> cursor = connection.scan(
                        ScanOptions.scanOptions()
                                .match(ORDER_COUNT_PREFIX + "*")
                                .count(100) // 한 번에 100개 힌트
                                .build()
                );
                cursor.forEachRemaining(key -> keysToDelete.add(new String(key)));
                return null;
            });
            if (!keysToDelete.isEmpty()) {
                stringRedisTemplate.delete(keysToDelete);
            }

            // Redis에 데이터 저장
            for (Object[] result : results) {
                Long groupBuyId = (Long) result[0];
                Integer orderCount = ((Long) result[1]).intValue();

                // Sorted Set에 저장 (랭킹용)
                stringRedisTemplate.opsForZSet().add(RANKING_KEY, groupBuyId.toString(), orderCount);

                // 개별 카운트 저장
                String countKey = ORDER_COUNT_PREFIX + groupBuyId;
                stringRedisTemplate.opsForValue().set(countKey, orderCount.toString());
            }

            log.info("Redis synchronization completed. Synced {} group buys", results.size());
        } catch (Exception e) {
            log.error("Failed to sync order counts from DB to Redis", e);
        }
    }
}
