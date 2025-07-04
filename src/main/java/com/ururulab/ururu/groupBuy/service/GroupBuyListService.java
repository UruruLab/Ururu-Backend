package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.ururulab.ururu.global.exception.error.ErrorCode.GROUPBUY_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyListService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final GroupBuyRankingService rankingService;


    /**
     * 카테고리별 공동구매 목록 조회 (주문량 기준 정렬)
     */
    public List<GroupBuyListResponse> getGroupBuyListOrderByOrderCount(Long categoryId, int limit) {
        log.debug("Fetching group buy list - categoryId: {}, limit: {}", categoryId, limit);

        // 1. 카테고리별 공동구매 조회
        List<GroupBuy> groupBuys = categoryId != null
                ? groupBuyRepository.findByProductCategoryId(categoryId)
                : groupBuyRepository.findAllPublic();

        // 2. 데이터가 없으면 예외 발생
        if (groupBuys.isEmpty()) {
            String message = categoryId != null
                    ? "해당 카테고리의 공동구매를 찾을 수 없습니다."
                    : "공동구매를 찾을 수 없습니다.";
            throw new BusinessException(GROUPBUY_NOT_FOUND, message);
        }

        // 2. 각 공동구매의 총 주문량 계산
        List<Long> groupBuyIds = groupBuys.stream().map(GroupBuy::getId).toList();
        Map<Long, Integer> orderCountMap = getOrderCountMap(groupBuyIds);

        // 3. 주문량 기준으로 정렬 후 Response 생성
        return groupBuys.stream()
                .sorted((a, b) -> {
                    Integer countA = orderCountMap.getOrDefault(a.getId(), 0);
                    Integer countB = orderCountMap.getOrDefault(b.getId(), 0);
                    return countB.compareTo(countA); // 주문량 많은 순
                })
                .limit(limit)
                .map(groupBuy -> {
                    List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);
                    Integer orderCount = orderCountMap.getOrDefault(groupBuy.getId(), 0);
                    return GroupBuyListResponse.from(groupBuy, options, orderCount);
                })
                .toList();
    }

    private Map<Long, Integer> getOrderCountMap(List<Long> groupBuyIds) {
        return rankingService.getOrderCounts(groupBuyIds);
    }
}
