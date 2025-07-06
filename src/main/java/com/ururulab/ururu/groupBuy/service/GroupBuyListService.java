package com.ururulab.ururu.groupBuy.service;

import com.querydsl.core.Tuple;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuySortOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.groupBuy.util.TimeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ururulab.ururu.global.exception.error.ErrorCode.GROUPBUY_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyListService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final GroupBuyRankingService rankingService;

    public List<GroupBuyListResponse> getGroupBuyList(Long categoryId, int limit, String sortType) {
        if ("order_count".equals(sortType)) {
            return getGroupBuyListOrderByOrderCount(categoryId, limit);
        }
        return getGroupBuyListWithSort(categoryId, limit, sortType);
    }

    // QueryDSL 적용
    public List<GroupBuyListResponse> getGroupBuyListWithSort(Long categoryId, int limit, String sortType) {
        log.debug("Fetching group buy list with sort - categoryId: {}, limit: {}, sortType: {}", categoryId, limit, sortType);

        if (!isValidSortType(sortType)) {
            log.warn("Invalid sort type: {}", sortType);
            throw new BusinessException(GROUPBUY_NOT_FOUND, "유효하지 않은 정렬 타입입니다.");
        }

        GroupBuySortOption sortOption = GroupBuySortOption.from(sortType); // enum 매핑
        List<Tuple> tuples = groupBuyRepository.findGroupBuysSorted(categoryId, sortOption, limit);

        if (tuples.isEmpty()) {
            String message = categoryId != null
                    ? "해당 카테고리의 공동구매를 찾을 수 없습니다."
                    : "공동구매를 찾을 수 없습니다.";
            throw new BusinessException(GROUPBUY_NOT_FOUND, message);
        }

        // 할인율 정렬이면 수동 정렬
        if ("discount".equals(sortType)) {
            return tuples.stream()
                    .map(this::convertToResponse)
                    .sorted(Comparator.comparing(GroupBuyListResponse::maxDiscountRate).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        // 나머지 정렬은 그대로 DTO 매핑
        return tuples.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private GroupBuyListResponse convertToResponse(Tuple row) {
        Long id = row.get(0, Long.class);
        String title = row.get(1, String.class);
        String thumbnailUrl = row.get(2, String.class);
        Integer displayFinalPrice = row.get(3, Integer.class);
        Integer startPrice = row.get(4, Integer.class);
        String discountStages = row.get(5, String.class);
        Instant endsAt = row.get(6, Instant.class);
        Long remainingSeconds = TimeCalculator.calculateRemainingSeconds(endsAt);
        Integer orderCount = row.get(7, Long.class).intValue();
        Instant createdAt = row.get(8, Instant.class);
        Integer maxDiscountRate = calculateMaxDiscountRate(discountStages);

        return new GroupBuyListResponse(
                id, title, thumbnailUrl, displayFinalPrice,
                startPrice, maxDiscountRate, endsAt,
                remainingSeconds, orderCount, createdAt
        );
    }



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

    /**
     * discount_stages JSON에서 최대 할인률 계산
     */
    private Integer calculateMaxDiscountRate(String discountStages) {
        if (discountStages == null || discountStages.isEmpty()) {
            return 0;
        }

        try {
            List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(discountStages);
            return stages.isEmpty() ? 0 : stages.get(stages.size() - 1).discountRate();
        } catch (Exception e) {
            log.warn("Failed to parse discount stages: {}", discountStages, e);
            return 0;
        }
    }

    /**
     * 유효한 정렬 타입인지 확인
     */
    private boolean isValidSortType(String sortType) {
        return sortType != null &&
                List.of("deadline", "discount", "latest", "price_low", "price_high").contains(sortType);
    }

}
