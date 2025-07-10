package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyMainService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;

    /**
     * 메인 화면 - 실시간 베스트 공동구매 조회 (판매량 기준 상위 3개)
     * 기존 GroupBuyListService 로직 재사용하여 일관성 유지
     * @return 판매량 많은 순으로 정렬된 상위 3개 공동구매 목록
     */
    @Cacheable(value = "realtimeBest", key = "'top3'")
    public List<GroupBuyListResponse> getRealtimeBestGroupBuys() {
        log.debug("Fetching realtime best 3 group buys for main page");

        // 전체 공개된 공동구매 조회 (기존 로직 재사용)
        List<GroupBuy> groupBuys = groupBuyRepository.findAllPublicWithOptions();

        if (groupBuys.isEmpty()) {
            log.warn("No public group buys found for realtime best");
            return List.of();
        }

        // 공동구매 ID 목록 추출
        List<Long> groupBuyIds = groupBuys.stream()
                .map(GroupBuy::getId)
                .toList();

        // 각 공동구매의 판매량 조회 (기존 로직 재사용)
        Map<Long, Integer> orderCountMap = getOrderCountMapFromInitialStock(groupBuyIds);

        // 판매량 기준 내림차순 정렬 후 상위 3개만 반환
        List<GroupBuyListResponse> bestList = groupBuys.stream()
                .sorted((a, b) -> {
                    Integer countA = orderCountMap.getOrDefault(a.getId(), 0);
                    Integer countB = orderCountMap.getOrDefault(b.getId(), 0);
                    // 판매량이 같으면 최신순(ID 역순)으로 정렬
                    int orderCountComparison = countB.compareTo(countA);
                    return orderCountComparison != 0 ? orderCountComparison : b.getId().compareTo(a.getId());
                })
                .limit(3) // 상위 3개만
                .map(groupBuy -> {
                    Integer orderCount = orderCountMap.getOrDefault(groupBuy.getId(), 0);
                    return GroupBuyListResponse.from(groupBuy, groupBuy.getOptions(), orderCount);
                })
                .collect(Collectors.toList());

        log.debug("Retrieved {} realtime best group buys for main page", bestList.size());
        return bestList;
    }

    /**
     * 메인 화면 - 카테고리별 인기 공동구매 조회 (판매량 기준 상위 6개)
     * 기존 GroupBuyListService 로직 재사용하여 일관성 유지
     * @param categoryId 카테고리 ID
     * @return 해당 카테고리의 판매량 많은 순으로 정렬된 상위 6개 공동구매 목록
     */
    @Cacheable(value = "categoryPopular", key = "#categoryId")
    public List<GroupBuyListResponse> getCategoryPopularGroupBuys(Long categoryId) {
        log.debug("Fetching popular group buys for category: {}", categoryId);

        // 해당 카테고리의 공개된 공동구매 조회 (기존 로직 재사용)
        List<GroupBuy> groupBuys = groupBuyRepository.findByProductCategoryIdWithOptions(categoryId);

        if (groupBuys.isEmpty()) {
            log.warn("No group buys found for category: {}", categoryId);
            return List.of();
        }

        // 공동구매 ID 목록 추출
        List<Long> groupBuyIds = groupBuys.stream()
                .map(GroupBuy::getId)
                .toList();

        // 각 공동구매의 판매량 조회 (기존 로직 재사용)
        Map<Long, Integer> orderCountMap = getOrderCountMapFromInitialStock(groupBuyIds);

        // 판매량 기준 내림차순 정렬 후 상위 6개만 반환
        List<GroupBuyListResponse> popularList = groupBuys.stream()
                .sorted((a, b) -> {
                    Integer countA = orderCountMap.getOrDefault(a.getId(), 0);
                    Integer countB = orderCountMap.getOrDefault(b.getId(), 0);
                    // 판매량이 같으면 최신순(ID 역순)으로 정렬
                    int orderCountComparison = countB.compareTo(countA);
                    return orderCountComparison != 0 ? orderCountComparison : b.getId().compareTo(a.getId());
                })
                .limit(6) // 상위 6개만
                .map(groupBuy -> {
                    Integer orderCount = orderCountMap.getOrDefault(groupBuy.getId(), 0);
                    return GroupBuyListResponse.from(groupBuy, groupBuy.getOptions(), orderCount);
                })
                .collect(Collectors.toList());

        log.debug("Retrieved {} popular group buys for category: {}", popularList.size(), categoryId);
        return popularList;
    }

    /**
     * 여러 공동구매의 판매량 조회 (GroupBuyListService에서 복사)
     * @param groupBuyIds 공동구매 ID 목록
     * @return 공동구매 ID별 판매량 맵
     */
    private Map<Long, Integer> getOrderCountMapFromInitialStock(List<Long> groupBuyIds) {
        List<Object[]> results = groupBuyOptionRepository.getTotalSoldQuantitiesByGroupBuyIds(groupBuyIds);

        Map<Long, Integer> orderCountMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // groupBuyId
                        row -> ((Long) row[1]).intValue() // totalSoldQuantity
                ));

        log.debug("Retrieved sold quantities for {} group buys using initialStock", groupBuyIds.size());
        return orderCountMap;
    }

}
