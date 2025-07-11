package com.ururulab.ururu.groupBuy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.Tuple;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuySortOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.common.CursorInfoDto;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyPageResponse;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static com.ururulab.ururu.global.exception.error.ErrorCode.GROUPBUY_NOT_FOUND;
import static com.ururulab.ururu.global.exception.error.ErrorCode.INVALID_SEARCH_KEYWORD;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyListService {

    private final GroupBuyRepository groupBuyRepository;
    private final ObjectMapper objectMapper;
    private final GroupBuyValidator groupBuyValidator;

    /**
     * 목록 조회 메인 cursor 적용
     * @param categoryId
     * @param limit
     * @param sortType
     * @param cursor
     * @return
     */
    public GroupBuyPageResponse getGroupBuyList(Long categoryId, int limit, String sortType, String cursor, String keyword) {
        if (!groupBuyValidator.isValidKeyword(keyword)) {
            log.warn("위험한 검색 키워드 차단: {}", keyword);
            throw new BusinessException(INVALID_SEARCH_KEYWORD);
        }

        String normalizedKeyword = normalizeKeyword(keyword);

        int fetchLimit = limit + 1;

        List<GroupBuyListResponse> items;

        if ("order_count".equals(sortType)) {
            items = getGroupBuyListOrderByOrderCountWithCursor(categoryId, fetchLimit, cursor, normalizedKeyword);
        } else {
            items = getGroupBuyListWithSortAndCursor(categoryId, fetchLimit, sortType, cursor, normalizedKeyword);
        }

        boolean hasMore = items.size() > limit;
        List<GroupBuyListResponse> trimmed = hasMore ? items.subList(0, limit) : items;

        String nextCursor = hasMore ? encodeCursor(trimmed.get(trimmed.size() - 1)) : null;

        return GroupBuyPageResponse.of(trimmed, nextCursor, hasMore);

    }

    /**
     * 키워드 정규화
     * 1. 공백 제거 (모든 종류의 공백: 스페이스, 탭, 줄바꿈 등)
     * 2. 소문자 변환
     * 3. 특수문자는 제거
     * 4. 길이 100자로 제한
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        // 길이 제한 (DoS 공격 방지)
        if (keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
            log.warn("키워드가 너무 깁니다. 100자로 제한됨: {}", keyword);
        }

        return keyword.replaceAll("[^\\p{L}\\p{N}\\s]", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }

    /**
     * QueryDSL 정렬 + 커서
     * @param categoryId
     * @param limit
     * @param sortType
     * @param cursor
     * @return
     */
    private List<GroupBuyListResponse> getGroupBuyListWithSortAndCursor(Long categoryId, int limit, String sortType, String cursor, String keyword) {
        log.debug("Fetching group buy list with sort and cursor - categoryId: {}, limit: {}, sortType: {}, cursor: {}",
                categoryId, limit, sortType, cursor);

        if (!isValidSortType(sortType)) {
            log.warn("Invalid sort type: {}", sortType);
            throw new BusinessException(GROUPBUY_NOT_FOUND, "유효하지 않은 정렬 타입입니다.");
        }

        GroupBuySortOption sortOption = GroupBuySortOption.from(sortType);
        CursorInfoDto cursorInfoDto = cursor != null ? decodeCursor(cursor) : null;

        List<Tuple> tuples = groupBuyRepository.findGroupBuysSortedWithCursor(
                categoryId, sortOption, limit + 1, cursorInfoDto, keyword);

        if (tuples.isEmpty() && cursor == null) {
            String message = categoryId != null
                    ? "해당 카테고리의 공동구매를 찾을 수 없습니다."
                    : "공동구매를 찾을 수 없습니다.";
            throw new BusinessException(GROUPBUY_NOT_FOUND, message);
        }

        return tuples.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 판매량 정렬 + 커서 + 키워드 검색
     * @param categoryId
     * @param limit
     * @param cursor
     * @param keyword
     * @return
     */
    private List<GroupBuyListResponse> getGroupBuyListOrderByOrderCountWithCursor(Long categoryId, int limit, String cursor, String keyword) {
        log.debug("Fetching group buy list by order count with cursor - categoryId: {}, limit: {}, cursor: {}, keyword: {}",
                categoryId, limit, cursor, keyword);

        GroupBuySortOption sortOption = GroupBuySortOption.ORDER_COUNT;
        CursorInfoDto cursorInfoDto = cursor != null ? decodeCursor(cursor) : null;

        // keyword까지 포함한 조회
        List<Tuple> tuples = groupBuyRepository.findGroupBuysSortedWithCursor(
                categoryId, sortOption, limit, cursorInfoDto, keyword);

        if (tuples.isEmpty() && cursor == null) {
            String message = categoryId != null
                    ? "해당 카테고리의 공동구매를 찾을 수 없습니다."
                    : "공동구매를 찾을 수 없습니다.";
            throw new BusinessException(GROUPBUY_NOT_FOUND, message);
        }

        // Tuple을 Response로 변환 후 판매량 기준 정렬
        List<GroupBuyListResponse> responses = tuples.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return responses.stream()
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
        Instant endsAt = row.get(5, Instant.class);
        Integer orderCount = row.get(6, Long.class).intValue();
        Instant createdAt = row.get(7, Instant.class);
        Integer maxDiscountRate = row.get(8, Integer.class);

        return new GroupBuyListResponse(
                id, title, thumbnailUrl, displayFinalPrice,
                startPrice, maxDiscountRate, endsAt,
                orderCount, createdAt
        );
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
