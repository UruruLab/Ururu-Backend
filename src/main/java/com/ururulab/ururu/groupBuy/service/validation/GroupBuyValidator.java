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
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.ProductOptionRepository;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import com.ururulab.ururu.seller.domain.entity.Seller;
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
    private final ProductRepository productRepository;

    public void validateCritical(GroupBuyRequest request, Long sellerId) {
        validateSchedule(request.endsAt());
        discountStageValidator.validateDiscountStages(request.discountStages());
        discountStageValidator.validateDiscountStageOrder(request.discountStages());
        validateProductOwnership(sellerId, request.productId());
        validateProductStatus(request.productId());

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
     * @param //startAt
     * @param endsAt
     */
    private void validateSchedule(Instant endsAt) {
        Instant now = Instant.now();

        // 종료일이 현재 시간보다 이후인지 검증
        if (endsAt.isBefore(now) || endsAt.equals(now)) {
            log.error("공동구매 종료일이 현재 시간보다 이전이거나 같습니다 - endAt: {}, now: {}", endsAt, now);
            throw new BusinessException(INVALID_END_TIME);
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
        Instant now = Instant.now(); //- mysql로 배포 시 변경

        // 종료일 검증
        if (groupBuy.getEndsAt().isBefore(now) || groupBuy.getEndsAt().equals(now)) {
            log.warn("공동구매 종료일이 현재 시간보다 이전이거나 같습니다 - endsAt: {}, now: {}",
                    groupBuy.getEndsAt(), now);
            throw new BusinessException(GROUPBUY_ALREADY_ENDED);
        }

        // 지속 시간 검증 (현재 시간부터 종료일까지)
        final long MIN_DURATION_HOURS = 1;
        final long MAX_DURATION_HOURS = Duration.ofDays(7).toHours(); // 1주일

        long durationHours = Duration.between(now, groupBuy.getEndsAt()).toHours();

        if (durationHours < MIN_DURATION_HOURS) {
            log.warn("공동구매 지속 시간이 너무 짧습니다 - durationHours: {}", durationHours);
            throw new BusinessException(GROUP_BUY_DURATION_TOO_SHORT);
        }
        if (durationHours > MAX_DURATION_HOURS) {
            log.warn("공동구매 지속 시간이 너무 깁니다 - durationHours: {}", durationHours);
            throw new BusinessException(GROUP_BUY_DURATION_TOO_LONG);
        }

        // 옵션 존재 검증
        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);
        if (options.isEmpty()) {
            log.warn("공동구매에 옵션이 없습니다 - groupBuyId: {}", groupBuy.getId());
            throw new BusinessException(GROUPBUY_NO_OPTIONS);
        }

        // 재고 검증
        boolean hasStock = options.stream().anyMatch(option -> option.getStock() > 0);
        if (!hasStock) {
            log.warn("공동구매에 재고가 없습니다 - groupBuyId: {}", groupBuy.getId());
            throw new BusinessException(GROUPBUY_NO_STOCK);
        }

        log.info("공동구매 오픈 조건 검증 성공 - groupBuyId: {}, durationHours: {}",
                groupBuy.getId(), durationHours);
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

    /**
     * 상품 소유자 검증
     * 해당 상품이 요청한 판매자의 것인지 확인
     * @param sellerId 요청한 판매자 ID
     * @param productId 상품 ID
     * @throws BusinessException 다른 판매자의 상품으로 공동구매를 생성하려는 경우
     */
    public void validateProductOwnership(Long sellerId, Long productId) {
        log.debug("상품 소유자 검증 시작: sellerId={}, productId={}", sellerId, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND, productId));

        if (!product.getSeller().getId().equals(sellerId)) {
            log.warn("다른 판매자의 상품으로 공동구매 생성 시도 - requestSellerId: {}, productSellerId: {}, productId: {}",
                    sellerId, product.getSeller().getId(), productId);
            throw new BusinessException(PRODUCT_SELLER_MISMATCH, productId);
        }

        log.info("상품 소유자 검증 성공: sellerId={}, productId={}", sellerId, productId);
    }

    /**
     * 상품 상태 검증
     * 공동구매에 사용할 수 있는 상품 상태인지 확인
     * @param productId 상품 ID
     */
    public void validateProductStatus(Long productId) {
        log.debug("상품 상태 검증 시작: productId={}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND, productId));

        // DELETED 상품은 공동구매에 사용할 수 없음
        if (product.getStatus() == Status.DELETED) {
            log.warn("삭제된 상품으로 공동구매 생성 시도: productId={}", productId);
            throw new BusinessException(PRODUCT_NOT_AVAILABLE, productId);
        }

        log.debug("상품 상태 검증 성공: productId={}, status={}", productId, product.getStatus());
    }


    /**
     * SQL Injection 방지
     * @param keyword
     * @return
     */
    public boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        // SQL Injection 방지 패턴
        String[] dangerousPatterns = {
                "script", "select", "insert", "update", "delete",
                "drop", "union", "exec", "javascript:", "vbscript:"
        };

        String lowerKeyword = keyword.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerKeyword.contains(pattern)) {
                log.warn("위험한 키워드 감지: {}", keyword);
                return false;
            }
        }

        return true;
    }

}
