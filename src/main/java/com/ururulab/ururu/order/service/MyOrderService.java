package com.ururulab.ururu.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyStatisticsRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.order.dto.response.MyOrderListResponseDto;
import com.ururulab.ururu.order.dto.response.MyOrderResponseDto;
import com.ururulab.ururu.order.dto.response.OrderItemResponseDto;
import com.ururulab.ururu.payment.domain.entity.Payment;
import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.RefundItemRepository;
import com.ururulab.ururu.payment.domain.repository.RefundRepository;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final GroupBuyStatisticsRepository groupBuyStatisticsRepository;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final ObjectMapper objectMapper;

    /**
     * 회원의 주문 목록을 조회합니다.
     * 환불되지 않은 OrderItem이 있는 주문만 반환하며,
     * 상태별 통계 정보도 함께 제공합니다.
     *
     * @param memberId 회원 ID
     * @param statusParam 주문 상태 필터 ("all" 또는 실제 상태값)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 주문 목록 및 통계 정보
     */
    @Transactional(readOnly = true)
    public MyOrderListResponseDto getMyOrders(Long memberId, String statusParam, int page, int size) {
        log.debug("나의 주문 목록 조회 - 회원ID: {}, 상태: {}, 페이지: {}, 크기: {}",
                memberId, statusParam, page, size);

        validateMemberExists(memberId);

        String statusFilter = parseStatusFilter(statusParam);

        // 주문 통계 조회
        Long inProgress = orderRepository.countMyOrders(memberId, "inprogress");
        Long confirmed = orderRepository.countMyOrders(memberId, "confirmed");
        Long refundPending = orderRepository.countMyOrders(memberId, "refundpending");

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Order> orders = orderRepository.findMyOrdersWithDetails(memberId, statusFilter, pageable);

        List<MyOrderResponseDto> orderDtos = orders.getContent().stream()
                .map(this::toMyOrderResponseDto)
                .toList();

        return new MyOrderListResponseDto(
                inProgress.intValue(),
                confirmed.intValue(),
                refundPending.intValue(),
                orderDtos,
                page,
                size,
                orders.getTotalElements()
        );
    }

    /**
     * 회원 존재 여부를 검증합니다.
     *
     * @param memberId 회원 ID
     * @throws BusinessException 회원이 존재하지 않는 경우
     */
    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    /**
     * 주문 상태 필터 파라미터를 파싱하고 검증합니다.
     *
     * @param statusParam 상태 파라미터 ("all", "inprogress", "confirmed", "refundpending")
     * @return 정규화된 상태 필터 문자열
     */
    private String parseStatusFilter(String statusParam) {
        if (statusParam == null || "all".equalsIgnoreCase(statusParam)) {
            return "all";
        }

        String status = statusParam.toLowerCase();
        if ("inprogress".equals(status) || "confirmed".equals(status) || "refundpending".equals(status)) {
            return status;
        }

        log.warn("잘못된 주문 상태 파라미터, all로 처리: {}", statusParam);
        return "all";
    }


    /**
     * Order 엔티티를 MyOrderResponseDto로 변환합니다.
     *
     * @param order 주문 엔티티
     * @return 주문 응답 DTO
     */
    private MyOrderResponseDto toMyOrderResponseDto(Order order) {
        // 환불되지 않은 OrderItem들만 필터링
        List<OrderItemResponseDto> orderItems = order.getOrderItems().stream()
                .filter(this::isNotRefunded)
                .map(this::toOrderItemResponseDto)
                .toList();

        Integer totalAmount = calculateCurrentAmount(order);

        Boolean[] refundStatus = calculateRefundStatus(order);
        Boolean canRefundChangeOfMind = refundStatus[0];
        Boolean canRefundOthers = refundStatus[1];

        Optional<Refund> activeRefund = refundRepository.findActiveRefundByOrderId(order.getId());

        return new MyOrderResponseDto(
                order.getId(),
                order.getCreatedAt(),
                order.getTrackingNumber(),
                totalAmount,
                canRefundChangeOfMind,
                canRefundOthers,
                activeRefund.map(Refund::getType).orElse(null),
                activeRefund.map(Refund::getReason).orElse(null),
                orderItems
        );
    }

    /**
     * 환불 가능 여부를 계산합니다. (기본 조건 + 기간 체크 통합)
     * 1. 주문 상태가 ORDERED 또는 PARTIAL_REFUNDED인 경우에만 환불 가능
     * 2. 운송장 등록 전에는 무조건 환불 가능
     * 3. 운송장 등록 후: 운송장 등록일로부터 사유별로 다른 기한
     *
     * @return [canRefundChangeOfMind, canRefundOthers]
     */
    private Boolean[] calculateRefundStatus(Order order) {
        if (order.getStatus() != OrderStatus.ORDERED &&
                order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
            return new Boolean[]{false, false};
        }

        if (order.getTrackingRegisteredAt() == null) {
            return new Boolean[]{true, true};
        }

        Instant now = Instant.now();
        Instant changeOfMindDeadline = order.getTrackingRegisteredAt().plus(7, ChronoUnit.DAYS);
        Instant othersDeadline = order.getTrackingRegisteredAt().plus(30, ChronoUnit.DAYS);

        Boolean canRefundChangeOfMind = now.isBefore(changeOfMindDeadline);
        Boolean canRefundOthers = now.isBefore(othersDeadline);

        return new Boolean[]{canRefundChangeOfMind, canRefundOthers};
    }

    /**
     * OrderItem이 환불되지 않았는지 확인합니다.*
     * INITIATED 상태만 "환불 진행중"으로 간주하고,
     * 나머지는 모두 "환불되지 않음"으로 처리합니다.
     *
     * @param orderItem 주문 아이템
     * @return 환불되지 않았으면 true
     */
    private boolean isNotRefunded(OrderItem orderItem) {
        // INITIATED가 아니면 모두 "환불 안됨"으로 처리
        return !refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                orderItem.getId(),
                List.of(
                        RefundStatus.APPROVED,
                        RefundStatus.COMPLETED,
                        RefundStatus.FAILED,
                        RefundStatus.REJECTED
                )
        );
    }

    /**
     * 현재 주문의 유효 금액을 계산합니다.
     * 전체 결제 금액에서 환불된 금액을 뺀 값을 반환합니다.
     *
     * @param order 주문 엔티티
     * @return 현재 유효 금액
     */
    private Integer calculateCurrentAmount(Order order) {
        // 전체 결제 금액
        Integer totalAmount = paymentRepository.findByOrderId(order.getId())
                .map(Payment::getTotalAmount)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 환불된 금액 계산
        Integer refundedAmount = order.getOrderItems().stream()
                .filter(this::isRefunded)
                .mapToInt(item -> item.getGroupBuyOption().getSalePrice() * item.getQuantity())
                .sum();

        return totalAmount - refundedAmount;
    }

    /**
     * OrderItem이 환불되었는지 확인합니다.
     * INITIATED가 아닌 모든 환불 상태를 "환불됨"으로 처리합니다.
     *
     * @param orderItem 주문 아이템
     * @return 환불되었으면 true
     */
    private boolean isRefunded(OrderItem orderItem) {
        return !refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                orderItem.getId(),
                List.of(RefundStatus.INITIATED)
        );
    }

    /**
     * OrderItem을 OrderItemResponseDto로 변환합니다.
     *
     * @param orderItem 주문 아이템
     * @return 주문 아이템 응답 DTO
     */
    private OrderItemResponseDto toOrderItemResponseDto(OrderItem orderItem) {
        GroupBuyOption groupBuyOption = orderItem.getGroupBuyOption();
        GroupBuy groupBuy = groupBuyOption.getGroupBuy();
        ProductOption productOption = groupBuyOption.getProductOption();

        String groupBuyStatus = determineGroupBuyStatus(groupBuy);
        Integer discountRate = calculateDiscountRate(groupBuy);

        return new OrderItemResponseDto(
                groupBuyOption.getId(),
                productOption.getId(),
                groupBuyStatus,
                discountRate,
                productOption.getImageUrl(),
                groupBuy.getProduct().getName(),
                productOption.getName(),
                orderItem.getQuantity(),
                groupBuyOption.getSalePrice()
        );
    }

    /**
     * 공구 상태를 결정합니다.
     *
     * @param groupBuy 공구 엔티티
     * @return 공구 상태 문자열
     */
    private String determineGroupBuyStatus(GroupBuy groupBuy) {
        switch (groupBuy.getStatus()) {
            case OPEN -> {
                return "OPEN";
            }
            case CLOSED -> {
                Optional<GroupBuyStatistics> statisticsOpt =
                        groupBuyStatisticsRepository.findByGroupBuyId(groupBuy.getId());

                if (statisticsOpt.isPresent()) {
                    return statisticsOpt.get().getFinalStatus().name();
                } else {
                    return "CLOSED";
                }
            }
            default -> {
                return "DRAFT";
            }
        }
    }

    /**
     * 공구의 할인율을 계산합니다.
     *
     * @param groupBuy 공구 엔티티
     * @return 할인율 (퍼센트)
     */
    private Integer calculateDiscountRate(GroupBuy groupBuy) {
        switch (groupBuy.getStatus()) {
            case OPEN -> {
                return calculateCurrentDiscountRate(groupBuy);
            }
            case CLOSED -> {
                Optional<GroupBuyStatistics> statisticsOpt =
                        groupBuyStatisticsRepository.findByGroupBuyId(groupBuy.getId());

                if (statisticsOpt.isPresent()) {
                    return statisticsOpt.get().getFinalDiscountRate();
                } else {
                    return 0;
                }
            }
            default -> {
                return 0;
            }
        }
    }

    /**
     * 진행중인 공구의 현재 할인율을 계산합니다.
     *
     * @param groupBuy 공구 엔티티
     * @return 현재 할인율
     */
    private Integer calculateCurrentDiscountRate(GroupBuy groupBuy) {
        try {
            String discountStagesJson = groupBuy.getDiscountStages();
            List<Map<String, Object>> stages = objectMapper.readValue(
                    discountStagesJson,
                    new TypeReference<>() {}
            );

            Integer totalSalesQuantity = orderItemRepository.getTotalQuantityByGroupBuyId(groupBuy.getId());

            int maxDiscountRate = 0;
            for (Map<String, Object> stage : stages) {
                Integer count = (Integer) stage.get("count");
                Integer rate = (Integer) stage.get("rate");

                if (totalSalesQuantity >= count && rate > maxDiscountRate) {
                    maxDiscountRate = rate;
                }
            }

            return maxDiscountRate;

        } catch (Exception e) {
            log.error("현재 할인율 계산 실패 - GroupBuy ID: {}, discountStages: {}",
                    groupBuy.getId(), groupBuy.getDiscountStages(), e);
            return 0;
        }
    }
}