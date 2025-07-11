package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyMainService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyRankingService groupBuyRankingService;

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

        List<GroupBuyListResponse> bestList = groupBuyRankingService.getTopGroupBuysByOrderCount(groupBuys, 3);

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

        List<GroupBuy> groupBuys = groupBuyRepository.findByProductCategoryIdWithOptions(categoryId);
        if (groupBuys.isEmpty()) {
            log.warn("No group buys found for category: {}", categoryId);
            return List.of();
        }

        List<GroupBuyListResponse> popularList = groupBuyRankingService.getTopGroupBuysByOrderCount(groupBuys, 6);

        log.debug("Retrieved {} popular group buys for category: {}", popularList.size(), categoryId);
        return popularList;
    }
}
