package com.ururulab.ururu.groupBuy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuySortOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.common.CursorInfoDto;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyPageResponse;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
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
    private final ObjectMapper objectMapper;

    /**
     * 목록 조회 메인 cursor 적용
     * @param categoryId
     * @param limit
     * @param sortType
     * @param cursor
     * @return
     */
    public GroupBuyPageResponse getGroupBuyList(Long categoryId, int limit, String sortType, String cursor) {
        int fetchLimit = limit + 1;

        List<GroupBuyListResponse> items;

        if ("order_count".equals(sortType)) {
            items = getGroupBuyListOrderByOrderCountWithCursor(categoryId, fetchLimit, cursor);
        } else {
            items = getGroupBuyListWithSortAndCursor(categoryId, fetchLimit, sortType, cursor);
        }

        boolean hasMore = items.size() > limit;
        List<GroupBuyListResponse> trimmed = hasMore ? items.subList(0, limit) : items;

        String nextCursor = hasMore ? encodeCursor(trimmed.get(trimmed.size() - 1)) : null;

        return GroupBuyPageResponse.of(trimmed, nextCursor, hasMore);

    }

    /**
     * QueryDSL 정렬 + 커서
     * @param categoryId
     * @param limit
     * @param sortType
     * @param cursor
     * @return
     */
    private List<GroupBuyListResponse> getGroupBuyListWithSortAndCursor(Long categoryId, int limit, String sortType, String cursor) {
        log.debug("Fetching group buy list with sort and cursor - categoryId: {}, limit: {}, sortType: {}, cursor: {}",
                categoryId, limit, sortType, cursor);

        if (!isValidSortType(sortType)) {
            log.warn("Invalid sort type: {}", sortType);
            throw new BusinessException(GROUPBUY_NOT_FOUND, "유효하지 않은 정렬 타입입니다.");
        }

        GroupBuySortOption sortOption = GroupBuySortOption.from(sortType);
        CursorInfoDto cursorInfoDto = cursor != null ? decodeCursor(cursor) : null;

        List<Tuple> tuples = groupBuyRepository.findGroupBuysSortedWithCursor(categoryId, sortOption, limit + 1, cursorInfoDto);

        if (tuples.isEmpty() && cursor == null) {
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
                    .collect(Collectors.toList());
        }

        return tuples.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 판매량 정렬 + 커서
     * @param categoryId
     * @param limit
     * @param cursor
     * @return
     */
    private List<GroupBuyListResponse> getGroupBuyListOrderByOrderCountWithCursor(Long categoryId, int limit, String cursor) {
        log.debug("Fetching group buy list by order count with cursor - categoryId: {}, limit: {}, cursor: {}",
                categoryId, limit, cursor);

        List<GroupBuy> groupBuys = categoryId != null
                ? groupBuyRepository.findByProductCategoryIdWithOptions(categoryId)
                : groupBuyRepository.findAllPublicWithOptions();

        if (groupBuys.isEmpty()) {
            String message = categoryId != null
                    ? "해당 카테고리의 공동구매를 찾을 수 없습니다."
                    : "공동구매를 찾을 수 없습니다.";
            throw new BusinessException(GROUPBUY_NOT_FOUND, message);
        }

        // initialStock 기반 판매량 배치 조회
        List<Long> groupBuyIds = groupBuys.stream().map(GroupBuy::getId).toList();
        Map<Long, Integer> orderCountMap = getOrderCountMapFromInitialStock(groupBuyIds);

        // 판매량 기준으로 정렬
        List<GroupBuyListResponse> sortedList = groupBuys.stream()
                .sorted((a, b) -> {
                    Integer countA = orderCountMap.getOrDefault(a.getId(), 0);
                    Integer countB = orderCountMap.getOrDefault(b.getId(), 0);
                    return b.getId().compareTo(a.getId());
                })
                .map(groupBuy -> {
                    Integer orderCount = orderCountMap.getOrDefault(groupBuy.getId(), 0);
                    return GroupBuyListResponse.from(groupBuy, groupBuy.getOptions(), orderCount);
                })
                .toList();

        // 커서 기반 페이징 적용
        if (cursor != null) {
            CursorInfoDto cursorInfoDto = decodeCursor(cursor);
            sortedList = sortedList.stream()
                    .filter(item -> {
                        // 주문량이 더 적거나, 같으면 id가 더 작은 것들만
                        if (item.orderCount() < cursorInfoDto.orderCount()) {
                            return true;
                        } else if (item.orderCount().equals(cursorInfoDto.orderCount())) {
                            return item.id() < cursorInfoDto.id();
                        }
                        return false;
                    })
                    .toList();
        }

        return sortedList.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 커서 인코딩
     * @param response
     * @return
     */
    private String encodeCursor(GroupBuyListResponse response) {
        try {
            CursorInfoDto cursorInfoDto = CursorInfoDto.from(response);
            String json = objectMapper.writeValueAsString(cursorInfoDto);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (Exception e) {
            log.error("Failed to encode cursor", e);
            throw new BusinessException(GROUPBUY_NOT_FOUND, "커서 인코딩에 실패했습니다.");
        }
    }

    /**
     * 커서 디코딩
     * @param cursor
     * @return
     */
    private CursorInfoDto decodeCursor(String cursor) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String json = new String(decodedBytes);
            return objectMapper.readValue(json, CursorInfoDto.class);
        } catch (Exception e) {
            log.error("Failed to decode cursor: {}", cursor, e);
            throw new BusinessException(GROUPBUY_NOT_FOUND, "유효하지 않은 커서입니다.");
        }
    }

    private GroupBuyListResponse convertToResponse(Tuple row) {
        Long id = row.get(0, Long.class);
        String title = row.get(1, String.class);
        String thumbnailUrl = row.get(2, String.class);
        Integer displayFinalPrice = row.get(3, Integer.class);
        Integer startPrice = row.get(4, Integer.class);
        String discountStages = row.get(5, String.class);
        Instant endsAt = row.get(6, Instant.class);
        Integer orderCount = row.get(7, Long.class).intValue();
        Instant createdAt = row.get(8, Instant.class);
        Integer maxDiscountRate = calculateMaxDiscountRate(discountStages);

        return new GroupBuyListResponse(
                id, title, thumbnailUrl, displayFinalPrice,
                startPrice, maxDiscountRate, endsAt,
                orderCount, createdAt
        );
    }

    /**
     * 여러 공동구매의 판매량 조회
     * @param groupBuyIds
     * @return
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

    /**
     * discount_stages JSON에서 최대 할인률 계산
     * @param discountStages
     * @return
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
     * @param sortType
     * @return
     */
    private boolean isValidSortType(String sortType) {
        return sortType != null &&
                List.of("deadline", "discount", "latest", "price_low", "price_high").contains(sortType);
    }
}
