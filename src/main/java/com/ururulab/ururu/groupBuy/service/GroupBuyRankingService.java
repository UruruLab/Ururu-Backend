package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRankingService {

    private final GroupBuyOptionRepository groupBuyOptionRepository;

    /**
     * 여러 공동구매의 판매량 조회
     * @param groupBuyIds 공동구매 ID 리스트
     * @return 공동구매별 판매량 Map
     */
    public Map<Long, Integer> getOrderCounts(List<Long> groupBuyIds) {
        log.debug("Retrieving sold quantities for {} group buys directly from DB", groupBuyIds.size());

        List<Object[]> results = groupBuyOptionRepository.getTotalSoldQuantitiesByGroupBuyIds(groupBuyIds);

        Map<Long, Integer> resultMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],           // groupBuyId
                        row -> ((Long) row[1]).intValue() // totalSoldQuantity
                ));

        log.debug("Successfully retrieved sold quantities for {} group buys", groupBuyIds.size());
        return resultMap;
    }

    /**
     * 공동구매 목록을 판매량 기준으로 정렬하여 GroupBuyListResponse로 변환
     * GroupBuyMainService에서 공통 사용
     *
     * @param groupBuys 정렬할 공동구매 목록
     * @param orderCountMap 판매량 데이터 (getOrderCounts()로 조회한 결과)
     * @param limit 반환할 최대 개수
     * @return 판매량 기준으로 정렬된 GroupBuyListResponse 목록
     */
    public List<GroupBuyListResponse> sortByOrderCount(List<GroupBuy> groupBuys,
                                                       Map<Long, Integer> orderCountMap,
                                                       int limit) {
        log.debug("Sorting {} group buys by order count, limit: {}", groupBuys.size(), limit);

        List<GroupBuyListResponse> sortedList = groupBuys.stream()
                .sorted((a, b) -> {
                    Integer countA = orderCountMap.getOrDefault(a.getId(), 0);
                    Integer countB = orderCountMap.getOrDefault(b.getId(), 0);
                    // 판매량 내림차순, 같으면 최신순(ID 역순)으로 정렬
                    int orderCountComparison = countB.compareTo(countA);
                    return orderCountComparison != 0 ? orderCountComparison : b.getId().compareTo(a.getId());
                })
                .limit(limit)
                .map(groupBuy -> {
                    Integer orderCount = orderCountMap.getOrDefault(groupBuy.getId(), 0);
                    return GroupBuyListResponse.from(groupBuy, groupBuy.getOptions(), orderCount);
                })
                .collect(Collectors.toList());

        log.debug("Successfully sorted and converted {} group buys to responses", sortedList.size());
        return sortedList;
    }

    /**
     * 공동구매 목록 조회 + 판매량 조회 + 정렬을 한번에 처리하는 편의 메서드
     *
     * @param groupBuys 정렬할 공동구매 목록
     * @param limit 반환할 최대 개수
     * @return 판매량 기준으로 정렬된 GroupBuyListResponse 목록
     */
    public List<GroupBuyListResponse> getTopGroupBuysByOrderCount(List<GroupBuy> groupBuys, int limit) {
        if (groupBuys.isEmpty()) {
            log.debug("Empty group buy list provided");
            return List.of();
        }

        // 1. 공동구매 ID 목록 추출
        List<Long> groupBuyIds = groupBuys.stream()
                .map(GroupBuy::getId)
                .toList();

        // 2. 판매량 조회
        Map<Long, Integer> orderCountMap = getOrderCounts(groupBuyIds);

        // 3. 정렬 및 변환
        return sortByOrderCount(groupBuys, orderCountMap, limit);
    }
}
