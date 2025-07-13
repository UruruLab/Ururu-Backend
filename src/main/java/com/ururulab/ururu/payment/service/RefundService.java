package com.ururulab.ururu.payment.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.payment.domain.entity.Payment;
import com.ururulab.ururu.payment.domain.entity.PointTransaction;
import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.RefundItem;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
import com.ururulab.ururu.payment.domain.repository.RefundRepository;
import com.ururulab.ururu.payment.dto.request.RefundProcessRequestDto;
import com.ururulab.ururu.payment.dto.request.RefundRequestDto;
import com.ururulab.ururu.payment.dto.response.RefundCreateResponseDto;
import com.ururulab.ururu.payment.dto.response.RefundProcessResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PaymentService paymentService;

    /**
     * 수동 환불 요청을 생성합니다.
     * Order 단위 전체 환불만 지원하며, 환불 타입에 따라 운송장 등록 전후 기간 제한을 적용합니다.
     * 운송장 등록 전인 경우 자동 승인되어 즉시 환불 처리됩니다.
     *
     * @param memberId 환불을 요청하는 회원 ID
     * @param orderId 환불 대상 주문 ID
     * @param request 환불 요청 정보 (타입, 사유, 반송 운송장)
     * @return 생성된 환불 정보
     * @throws BusinessException 주문이 존재하지 않거나, 환불 불가 상태이거나, 중복 요청인 경우
     */
    @Transactional
    public RefundCreateResponseDto createRefundRequest(Long memberId, String orderId, RefundRequestDto request) {
        log.debug("수동 환불 요청 생성 - 회원ID: {}, 주문ID: {}, 타입: {}", memberId, orderId, request.type());

        Order order = findOrderById(orderId);
        validateOrderOwnership(order, memberId);
        validateOrderForRefund(order);
        validateNoDuplicateRefund(orderId, memberId);

        List<OrderItem> refundableItems = orderItemRepository.findRefundableItemsByOrderId(orderId);
        if (refundableItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEMS_EMPTY);
        }

        RefundType refundType = RefundType.from(request.type());
        boolean autoApprove = validateRefundPeriod(order, refundType);

        Payment payment = findPaymentByOrderId(orderId);
        Integer itemAmount = calculateItemAmount(refundableItems);
        Integer pointAmount = calculateRemainingPoint(payment, orderId);

        RefundStatus refundStatus = autoApprove ? RefundStatus.APPROVED : RefundStatus.INITIATED;

        Refund refund = Refund.create(payment, refundType, request.reason(),
                itemAmount, pointAmount, request.returnTrackingNumber(), refundStatus);

        refundableItems.forEach(orderItem -> {
            RefundItem refundItem = RefundItem.create(orderItem);
            refund.addRefundItem(refundItem);
        });

        Refund savedRefund = refundRepository.save(refund);

        if (autoApprove) {
            processRefundApproval(savedRefund);
            log.debug("자동 승인으로 환불 처리 완료 - 환불ID: {}", savedRefund.getId());
        } else {
            log.debug("수동 환불 요청 생성 완료 - 환불ID: {}, 판매자 승인 대기", savedRefund.getId());
        }

        return new RefundCreateResponseDto(
                savedRefund.getId(),
                savedRefund.getStatus(),
                savedRefund.getType(),
                itemAmount + pointAmount
        );
    }

    /**
     * 판매자의 환불 요청 처리를 수행합니다.
     * 승인 시 포인트 복구, 재고 복구, 주문 상태 변경, PG 환불 요청을 순차적으로 진행합니다.
     * 거절 시 환불 상태를 REJECTED로 변경하고 거절 사유를 기록합니다.
     *
     * @param sellerId 환불을 처리하는 판매자 ID
     * @param refundId 처리할 환불 ID
     * @param request 처리 요청 정보 (승인/거절, 거절 사유)
     * @return 처리 결과 정보
     * @throws BusinessException 환불이 존재하지 않거나, 이미 처리되었거나, 권한이 없는 경우
     */
    @Transactional
    public RefundProcessResponseDto processRefundRequest(Long sellerId, String refundId, RefundProcessRequestDto request) {
        log.debug("환불 요청 처리 - 판매자ID: {}, 환불ID: {}, 액션: {}", sellerId, refundId, request.action());

        Refund refund = findRefundById(refundId);
        validateRefundForProcessing(refund);
        validateSellerAuthority(refund, sellerId);

        if ("APPROVE".equals(request.action())) {
            refund.markAsApproved();
            return processRefundApproval(refund);
        } else if ("REJECT".equals(request.action())) {
            refund.markAsRejected(request.rejectReason());
            return processRefundRejection(refund, request.rejectReason());
        } else {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "액션은 APPROVE 또는 REJECT만 가능합니다.");
        }
    }

    /**
     * 환불 승인 처리 로직을 수행합니다.
     * 포인트 복구, 재고 복구, 주문/결제 상태 업데이트, PG 환불 요청을 순차적으로 처리합니다.
     *
     * @param refund 승인 처리할 환불 엔티티
     * @return 승인 처리 결과
     */
    private RefundProcessResponseDto processRefundApproval(Refund refund) {
        log.debug("환불 승인 처리 시작 - 환불ID: {}", refund.getId());

        restorePointsToCustomer(refund);
        restoreStockToInventory(refund);
        updateOrderAndPaymentStatus(refund);
        requestPgRefund(refund);

        log.debug("환불 승인 처리 완료 - 환불ID: {}", refund.getId());

        return new RefundProcessResponseDto(
                refund.getId(),
                refund.getPayment().getOrder().getId(),
                refund.getStatus(),
                null
        );
    }

    /**
     * 환불 거절 처리 로직을 수행합니다.
     * 환불 상태를 REJECTED로 변경하고 거절 사유를 기록합니다.
     *
     * @param refund 거절 처리할 환불 엔티티
     * @param rejectReason 거절 사유
     * @return 거절 처리 결과
     */
    private RefundProcessResponseDto processRefundRejection(Refund refund, String rejectReason) {
        log.debug("환불 거절 처리 - 환불ID: {}, 사유: {}", refund.getId(), rejectReason);

        return new RefundProcessResponseDto(
                refund.getId(),
                refund.getPayment().getOrder().getId(),
                refund.getStatus(),
                rejectReason
        );
    }

    /**
     * 고객에게 환불 포인트를 복구합니다.
     * 회원의 포인트를 증가시키고 포인트 거래 내역을 기록합니다.
     *
     * @param refund 포인트 복구 대상 환불 엔티티
     * @throws BusinessException 회원이 존재하지 않는 경우
     */
    private void restorePointsToCustomer(Refund refund) {
        Integer pointsToRestore = refund.getPoint();

        if (pointsToRestore > 0) {
            Member member = refund.getPayment().getMember();

            int updatedRows = memberRepository.increasePoints(member.getId(), pointsToRestore);
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
            }

            PointTransaction pointTransaction = PointTransaction.createEarned(
                    member, PointSource.REFUND, pointsToRestore, "환불로 인한 포인트 복구"
            );
            pointTransactionRepository.save(pointTransaction);

            log.debug("포인트 복구 완료 - 회원ID: {}, 복구 포인트: {}P", member.getId(), pointsToRestore);
        }
    }

    /**
     * 환불된 상품의 재고를 복구합니다.
     * 각 환불 아이템의 수량만큼 해당 공동구매 옵션의 재고를 증가시킵니다.
     *
     * @param refund 재고 복구 대상 환불 엔티티
     */
    private void restoreStockToInventory(Refund refund) {
        refund.getRefundItems().forEach(refundItem -> {
            Long optionId = refundItem.getOrderItem().getGroupBuyOption().getId();
            Integer quantity = refundItem.getOrderItem().getQuantity();

            int updatedRows = groupBuyOptionRepository.increaseStock(optionId, quantity);
            if (updatedRows == 0) {
                log.warn("재고 복구 실패 - 옵션ID: {}, 수량: {}", optionId, quantity);
            } else {
                log.debug("재고 복구 완료 - 옵션ID: {}, 복구 수량: {}개", optionId, quantity);
            }
        });
    }

    /**
     * 수동 환불에 따른 주문 및 결제 상태를 업데이트합니다.
     * 수동 환불은 항상 Order 단위 전체 환불이므로 상태를 REFUNDED로 변경합니다.
     *
     * @param refund 상태 업데이트 대상 환불 엔티티
     */
    private void updateOrderAndPaymentStatus(Refund refund) {
        Payment payment = refund.getPayment();
        Order order = payment.getOrder();

        order.changeStatus(OrderStatus.REFUNDED, "수동 환불 완료");
        payment.markAsRefunded(Instant.now());

        log.debug("주문/결제 상태 업데이트 완료 - 주문ID: {}", order.getId());
    }

    /**
     * PG 환불 요청을 처리합니다.
     * PaymentService를 통해 토스페이먼츠 환불 API를 호출합니다.
     * 환불 실패 시 상태를 FAILED로 변경하고 별도 재시도 로직에서 처리됩니다.
     *
     * @param refund PG 환불 요청 대상 환불 엔티티
     */
    private void requestPgRefund(Refund refund) {
        try {
            // TODO: PaymentService의 PG 환불 메서드 호출 구현 예정
            // paymentService.requestRefundToToss(refund);
            log.debug("PG 환불 요청 완료 - 환불ID: {}", refund.getId());
        } catch (Exception e) {
            log.error("PG 환불 요청 실패 - 환불ID: {}", refund.getId(), e);
            refund.markAsFailed();
        }
    }

    /**
     * 환불 기간을 검증하고 자동 승인 여부를 결정합니다.
     * 운송장 등록 전이면 자동 승인, 등록 후라면 환불 타입에 따라 기간 제한을 적용합니다.
     *
     * @param order 환불 대상 주문
     * @param refundType 환불 타입
     * @return 자동 승인 여부 (true: 자동 승인, false: 판매자 승인 필요)
     * @throws BusinessException 환불 기간이 만료된 경우
     */
    private boolean validateRefundPeriod(Order order, RefundType refundType) {
        if (order.getTrackingRegisteredAt() == null) {
            return true;
        }

        int allowedDays = (refundType == RefundType.CHANGE_OF_MIND) ? 7 : 30;
        Instant deadline = order.getTrackingRegisteredAt().plus(allowedDays, ChronoUnit.DAYS);

        if (Instant.now().isAfter(deadline)) {
            throw new BusinessException(ErrorCode.REFUND_PERIOD_EXPIRED, allowedDays);
        }

        return false;
    }

    /**
     * 환불 대상 상품들의 금액을 계산합니다.
     *
     * @param refundableItems 환불 대상 주문 아이템 목록
     * @return 총 상품 금액
     */
    private Integer calculateItemAmount(List<OrderItem> refundableItems) {
        return refundableItems.stream()
                .mapToInt(item -> item.getGroupBuyOption().getSalePrice() * item.getQuantity())
                .sum();
    }

    /**
     * 환불 가능한 남은 포인트를 계산합니다.
     * 전체 사용 포인트에서 이미 다른 환불로 복구된 포인트를 제외합니다.
     *
     * @param payment 결제 정보
     * @param orderId 주문 ID
     * @return 환불 가능한 남은 포인트
     */
    private Integer calculateRemainingPoint(Payment payment, String orderId) {
        Integer totalUsedPoint = payment.getPoint();

        Integer alreadyRefundedPoint = refundRepository.findByOrderId(orderId)
                .stream()
                .filter(refund -> refund.getStatus() == RefundStatus.APPROVED ||
                        refund.getStatus() == RefundStatus.COMPLETED)
                .mapToInt(Refund::getPoint)
                .sum();

        return totalUsedPoint - alreadyRefundedPoint;
    }

    /**
     * 주문 ID로 주문을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 엔티티
     * @throws BusinessException 주문이 존재하지 않는 경우
     */
    private Order findOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 환불 ID로 환불을 조회합니다.
     *
     * @param refundId 환불 ID
     * @return 환불 엔티티
     * @throws BusinessException 환불이 존재하지 않는 경우
     */
    private Refund findRefundById(String refundId) {
        return refundRepository.findByIdWithDetails(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND));
    }

    /**
     * 주문 ID로 결제 정보를 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 결제 엔티티
     * @throws BusinessException 결제 정보가 존재하지 않는 경우
     */
    private Payment findPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * 주문 소유권을 검증합니다.
     *
     * @param order 검증할 주문
     * @param memberId 요청 회원 ID
     * @throws BusinessException 주문 소유자가 아닌 경우
     */
    private void validateOrderOwnership(Order order, Long memberId) {
        if (!order.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 주문이 환불 가능한 상태인지 검증합니다.
     *
     * @param order 검증할 주문
     * @throws BusinessException 환불 불가능한 주문 상태인 경우
     */
    private void validateOrderForRefund(Order order) {
        if (order.getStatus() != OrderStatus.ORDERED && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PENDING);
        }
    }

    /**
     * 중복 환불 요청이 없는지 검증합니다.
     *
     * @param orderId 주문 ID
     * @param memberId 요청 회원 ID
     * @throws BusinessException 이미 진행 중인 환불 요청이 있는 경우
     */
    private void validateNoDuplicateRefund(String orderId, Long memberId) {
        Optional<Refund> existingRefund = refundRepository.findActiveManualRefundByOrderAndMember(orderId, memberId);
        if (existingRefund.isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_REFUND_REQUEST);
        }
    }

    /**
     * 환불이 처리 가능한 상태인지 검증합니다.
     *
     * @param refund 검증할 환불
     * @throws BusinessException 처리 불가능한 환불 상태인 경우
     */
    private void validateRefundForProcessing(Refund refund) {
        if (!refund.isInitiated()) {
            throw new BusinessException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
    }

    /**
     * 판매자가 해당 환불을 처리할 권한이 있는지 검증합니다.
     *
     * @param refund 검증할 환불
     * @param sellerId 요청 판매자 ID
     * @throws BusinessException 처리 권한이 없는 경우
     */
    private void validateSellerAuthority(Refund refund, Long sellerId) {
        boolean hasAuthority = refund.getRefundItems().stream()
                .anyMatch(refundItem -> {
                    Long refundSellerId = refundItem.getOrderItem()
                            .getGroupBuyOption()
                            .getGroupBuy()
                            .getSeller()
                            .getId();
                    return refundSellerId.equals(sellerId);
                });

        if (!hasAuthority) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}