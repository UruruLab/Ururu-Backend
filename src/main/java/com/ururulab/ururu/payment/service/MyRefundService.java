package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.RefundItem;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.repository.RefundRepository;
import com.ururulab.ururu.payment.dto.response.MyRefundListResponseDto;
import com.ururulab.ururu.payment.dto.response.MyRefundResponseDto;
import com.ururulab.ururu.payment.dto.response.RefundItemResponseDto;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyRefundService {

    private final RefundRepository refundRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public MyRefundListResponseDto getMyRefunds(Long memberId, String statusParam, int page, int size) {
        log.debug("나의 환불 내역 조회 - 회원ID: {}, 상태: {}, 페이지: {}, 크기: {}",
                memberId, statusParam, page, size);

        validateMemberExists(memberId);

        RefundStatus status = parseRefundStatus(statusParam);

        Page<Refund> refundPage = getRefundsWithPaging(memberId, status, page, size);

        List<MyRefundResponseDto> refunds = refundPage.getContent().stream()
                .map(this::toMyRefundResponseDto)
                .toList();

        return new MyRefundListResponseDto(
                refunds,
                page,
                size,
                refundPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    protected Page<Refund> getRefundsWithPaging(Long memberId, RefundStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return refundRepository.findProcessedRefundsByMemberId(memberId, status, pageable);
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private RefundStatus parseRefundStatus(String statusParam) {
        if (statusParam == null || "all".equalsIgnoreCase(statusParam)) {
            return null;
        }
        try {
            return RefundStatus.from(statusParam);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 환불 상태 파라미터, all로 처리: {}", statusParam);
            return null;
        }
    }

    private MyRefundResponseDto toMyRefundResponseDto(Refund refund) {
        List<RefundItemResponseDto> refundItems = refund.getRefundItems().stream()
                .map(this::toRefundItemResponseDto)
                .toList();

        return new MyRefundResponseDto(
                refund.getId(),
                refund.getCreatedAt(),
                refund.getType(),
                refund.getReason(),
                refund.getStatus(),
                refund.getRejectReason(),
                refund.getRefundedAt(),
                refund.getAmount(),
                refundItems
        );
    }

    private RefundItemResponseDto toRefundItemResponseDto(RefundItem refundItem) {
        OrderItem orderItem = refundItem.getOrderItem();
        GroupBuyOption groupBuyOption = orderItem.getGroupBuyOption();
        ProductOption productOption = groupBuyOption.getProductOption();
        Product product = groupBuyOption.getGroupBuy().getProduct();

        return new RefundItemResponseDto(
                groupBuyOption.getId(),
                productOption.getId(),
                productOption.getImageUrl(),
                product.getName(),
                productOption.getName(),
                orderItem.getQuantity(),
                groupBuyOption.getSalePrice()
        );
    }
}