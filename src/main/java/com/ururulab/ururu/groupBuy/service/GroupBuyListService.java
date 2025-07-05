package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    public List<GroupBuyListResponse> getGroupBuyListWithSort(Long categoryId, int limit, String sortType) {
        log.debug("Fetching group buy list with sort - categoryId: {}, limit: {}, sortType: {}", categoryId, limit, sortType);

        // 유효하지 않은 정렬 타입인 경우 예외 발생 또는 기본값 처리
        if (!isValidSortType(sortType)) {
            log.warn("Invalid sort type: {}", sortType);
            throw new BusinessException(GROUPBUY_NOT_FOUND, "유효하지 않은 정렬 타입입니다.");
        }

        // 네이티브 쿼리를 통한 정렬된 데이터 조회 (Object[] 방식)
        List<Object[]> results = switch (sortType) {
            case "deadline" -> groupBuyRepository.findByCategoryIdOrderByDeadline(categoryId, limit);
            case "discount" -> groupBuyRepository.findByCategoryIdOrderByDiscount(categoryId, limit);
            case "latest" -> groupBuyRepository.findByCategoryIdOrderByLatest(categoryId, limit);
            case "price_low" -> groupBuyRepository.findByCategoryIdOrderByPriceLow(categoryId, limit);
            case "price_high" -> groupBuyRepository.findByCategoryIdOrderByPriceHigh(categoryId, limit);
            default -> throw new IllegalStateException("Unexpected sort type: " + sortType);
        };

        // 데이터가 없으면 예외 발생
        if (results.isEmpty()) {
            String message = categoryId != null
                    ? "해당 카테고리의 공동구매를 찾을 수 없습니다."
                    : "공동구매를 찾을 수 없습니다.";
            throw new BusinessException(GROUPBUY_NOT_FOUND, message);
        }

        return results.stream()
                .map(this::convertFromObjectArray)
                .collect(Collectors.toList());
    }

    /**
     * Object[] 배열을 GroupBuyListResponse로 변환
     */
    private GroupBuyListResponse convertFromObjectArray(Object[] row) {
        // Object[] 인덱스 순서:
        // 0: id, 1: title, 2: thumbnailUrl, 3: displayFinalPrice, 4: startPrice,
        // 5: discountStages, 6: endsAt, 7: remainingTimeSeconds, 8: orderCount, 9: createdAt


        Long id = (Long) row[0];
        String title = (String) row[1];
        String thumbnailUrl = (String) row[2];
        Integer displayFinalPrice = (Integer) row[3];
        Integer startPrice = (Integer) row[4];
        String discountStages = (String) row[5];
        OffsetDateTime endsAt = (OffsetDateTime) row[6]; //H2용
        Long remainingTimeSeconds = (Long) row[7];
        Long orderCount = (Long) row[8];
        OffsetDateTime createdAt = (OffsetDateTime) row[9]; //H2용

        // discount_stages JSON에서 실제 최대 할인률 계산
        Integer maxDiscountRate = calculateMaxDiscountRate(discountStages);

        return new GroupBuyListResponse(
                id,
                title,
                thumbnailUrl,
                displayFinalPrice,
                startPrice,
                maxDiscountRate, // 계산된 실제 값
                endsAt.toInstant(),
                remainingTimeSeconds,
                orderCount.intValue(),
                createdAt.toInstant()
        );
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
