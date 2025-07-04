package com.ururulab.ururu.groupBuy.service.validation;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyOptionRequest;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupBuyValidator {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyDiscountStageValidator discountStageValidator;
    private final GroupBuyOptionRepository groupBuyOptionRepository;

    public void validateCritical(GroupBuyRequest request) {
        validateSchedule(request.startAt(), request.endsAt());
        discountStageValidator.validateDiscountStages(request.discountStages());
        discountStageValidator.validateDiscountStageOrder(request.discountStages());

        int totalStock = request.options().stream()
                .mapToInt(GroupBuyOptionRequest::stock)
                .sum();

        discountStageValidator.validateMinQuantityAgainstStock(request.discountStages(), totalStock);

        //if (groupBuyRepository.existsGroupBuyByProduct(request.productId())) {
        if (groupBuyRepository.existsByProductIdAndStatusNot(request.productId(), GroupBuyStatus.CLOSED)) {
            throw new BusinessException(OVERLAPPING_GROUP_BUY_EXISTS);
        }

    }

    /**
     * 공동구매 시작일과 종료일 검증
     * @param startAt
     * @param endsAt
     */
    private void validateSchedule(Instant startAt, Instant endsAt) {
        Instant now = Instant.now();

        // 시작일이 현재 시간보다 이전인지 검증
        if (startAt.isBefore(now)) {
            log.error("공동구매 시작일이 현재 시간보다 이전입니다 - startAt: {}", startAt);
            throw new BusinessException(INVALID_START_TIME);
        }

        // 종료일이 시작일보다 이후인지 검증 (DTO에서도 검증하지만 한번 더)
        if (endsAt.isBefore(startAt) || endsAt.equals(startAt)) {
            log.error("공동구매 종료일이 시작일보다 이전이거나 같습니다 - startAt: {}, endAt: {}", startAt, endsAt);
            throw new BusinessException(INVALID_END_TIME);
        }

        final long MIN_DURATION_HOURS = 1;
        final long MAX_DURATION_HOURS = Duration.ofDays(7).toHours(); // 1주일

        long durationHours = Duration.between(startAt, endsAt).toHours();

        if (durationHours < MIN_DURATION_HOURS) {
            throw new BusinessException(GROUP_BUY_DURATION_TOO_SHORT);
        }
        if (durationHours > MAX_DURATION_HOURS) {
            throw new BusinessException(GROUP_BUY_DURATION_TOO_LONG);
        }

    }

    /**
     * 판매자 접근 권한 검증
     * 해당 공동구매가 요청한 판매자의 것인지 확인
     * @param sellerId 요청한 판매자 ID
     * @param groupBuy 조회된 공동구매
     * @throws BusinessException 다른 판매자의 공동구매에 접근하려는 경우
     */
    public void validateSellerAccess(Long sellerId, GroupBuy groupBuy) {
        if (!groupBuy.getSeller().getId().equals(sellerId)) {
            log.warn("Unauthorized seller access attempt - requestSellerId: {}, groupBuySellerId: {}, groupBuyId: {}",
                    sellerId, groupBuy.getSeller().getId(), groupBuy.getId());
            throw new BusinessException(GROUPBUY_SELLER_ACCESS_DENIED, groupBuy.getId());
        }

        log.debug("Seller access validated successfully - sellerId: {}, groupBuyId: {}",
                sellerId, groupBuy.getId());
    }

    /**
     * 상태 업데이트 요청 시 검증
     * @param request
     */
    public void validateStatusUpdateRequest(GroupBuyStatusUpdateRequest request) {
        if (!request.isValidStatusChange()) {
            throw new BusinessException(INVALID_STATUS_CHANGE, request.status());
        }
    }

    /**
     * 상태 변경 가능 여부 검즘 (STATUS가 DRAFT일 때만 OPEN으로 변경 가능)
     * @param currentStatus
     * @param newStatus
     */
    public void validateStatusChange(GroupBuyStatus currentStatus, GroupBuyStatus newStatus) {
        // DRAFT → OPEN만 허용
        if (currentStatus != GroupBuyStatus.DRAFT || newStatus != GroupBuyStatus.OPEN) {
            log.warn("Invalid status change attempt - from: {} to: {}", currentStatus, newStatus);
            throw new BusinessException(INVALID_STATUS_TRANSITION, currentStatus, newStatus);
        }
    }

    /**
     * 공동구매 OPEN으로 변경 시 검증 (시작일보다 이전 시간에 오픈할 수 없음)
     * @param groupBuy
     */
    public void validateGroupBuyOpenConditions(GroupBuy groupBuy) {
        //Instant now = Instant.now();
        Instant now = Instant.now().plusSeconds(9 * 3600); // UTC + 9시간 = KST;

        // 1. 시작일 검증
        if (groupBuy.getStartAt().isAfter(now)) {
            throw new BusinessException(GROUPBUY_NOT_STARTED_YET);
        }

        // 2. 종료일 검증
        if (groupBuy.getEndsAt().isBefore(now)) {
            throw new BusinessException(GROUPBUY_ALREADY_ENDED);
        }

        // 3. 옵션 존재 검증
        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);
        if (options.isEmpty()) {
            throw new BusinessException(GROUPBUY_NO_OPTIONS);
        }

        // 4. 재고 검증
        boolean hasStock = options.stream().anyMatch(option -> option.getStock() > 0);
        if (!hasStock) {
            throw new BusinessException(GROUPBUY_NO_STOCK);
        }

        log.debug("Group buy open conditions validated successfully - groupBuyId: {}", groupBuy.getId());
    }

}
