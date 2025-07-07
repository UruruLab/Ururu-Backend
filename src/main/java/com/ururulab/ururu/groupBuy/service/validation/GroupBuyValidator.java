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
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupBuyValidator {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyDiscountStageValidator discountStageValidator;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final ProductOptionRepository productOptionRepository;
    private final SellerRepository sellerRepository;

    public void validateCritical(GroupBuyRequest request) {
        validateSchedule(request.startAt(), request.endsAt());
        discountStageValidator.validateDiscountStages(request.discountStages());
        discountStageValidator.validateDiscountStageOrder(request.discountStages());

        // 상품 옵션 검증 추가
        List<Long> optionIds = request.options().stream()
                .map(GroupBuyOptionRequest::productOptionId)
                .toList();
        validateOptionsBelongToProduct(request.productId(), optionIds);

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
     * 판매자 존재 여부 검증
     * @param sellerId 판매자 ID
     * @throws BusinessException 판매자가 존재하지 않는 경우
     */
    public void validateSellerExists(Long sellerId) {
        if (!sellerRepository.existsById(sellerId)) {
            log.error("Seller not found with ID: {}", sellerId);
            throw new BusinessException(SELLER_NOT_FOUND, sellerId);
        }
        log.debug("Seller existence validated successfully - sellerId: {}", sellerId);
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
        //Instant now = Instant.now(); - mysql로 배포 시 변경
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

    /**
     * 옵션들이 해당 상품에 속하는지 검증
     * 단일 옵션 또는 여러 옵션 모두 처리 가능
     * @param productId 상품 ID
     * @param optionIds 검증할 옵션 ID 리스트
     */
    public void validateOptionsBelongToProduct(Long productId, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            log.debug("옵션 ID가 없어 검증을 건너뜁니다.");
            return;
        }

        log.debug("상품 옵션 소속 검증 시작: productId={}, optionIds={}", productId, optionIds);

        // 해당 상품에 속한 옵션 ID들 조회
        Set<Long> validOptionIds = productOptionRepository.findByProductId(productId)
                .stream()
                .map(ProductOption::getId)
                .collect(Collectors.toSet());

        log.debug("상품에 속한 유효한 옵션 ID: {}", validOptionIds);

        // 요청된 옵션 ID들이 모두 해당 상품에 속하는지 검증
        Set<Long> invalidOptionIds = optionIds.stream()
                .filter(optionId -> !validOptionIds.contains(optionId))
                .collect(Collectors.toSet());

        if (!invalidOptionIds.isEmpty()) {
            log.warn("상품에 속하지 않는 옵션이 포함되어 있습니다: productId={}, invalidOptionIds={}", productId, invalidOptionIds);
            throw new BusinessException(PRODUCT_OPTION_NOT_FOUND);
        }

        log.info("상품 옵션 소속 검증 성공: productId={}, 검증된 옵션 수={}", productId, optionIds.size());
    }


}
