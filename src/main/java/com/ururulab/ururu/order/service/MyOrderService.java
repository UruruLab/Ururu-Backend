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
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.RefundItemRepository;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RefundItemRepository refundItemRepository;  // 추가 필요
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public MyOrderListResponseDto getMyOrders(Long memberId, String statusParam, int page, int size) {
        log.debug("나의 주문 목록 조회 - 회원ID: {}, 상태: {}, 페이지: {}, 크기: {}",
                memberId, statusParam, page, size);

        validateMemberExists(memberId);

        OrderStatus status = parseOrderStatus(statusParam);

        OrderRepository.OrderStatisticsProjection statistics = getOrderStatistics(memberId);
        Page<Order> orders = getOrdersWithPaging(memberId, status, page, size);

        List<MyOrderResponseDto> orderDtos = orders.getContent().stream()
                .map(this::toMyOrderResponseDto)
                .toList();

        return new MyOrderListResponseDto(
                statistics.getInProgress().intValue(),
                statistics.getConfirmed().intValue(),
                statistics.getRefundPending().intValue(),
                orderDtos,
                page,
                size,
                orders.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    protected OrderRepository.OrderStatisticsProjection getOrderStatistics(Long memberId) {
        return orderRepository.getOrderStatisticsByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    protected Page<Order> getOrdersWithPaging(Long memberId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return orderRepository.findMyOrdersWithDetails(memberId, status, pageable);
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private OrderStatus parseOrderStatus(String statusParam) {
        if (statusParam == null || "all".equalsIgnoreCase(statusParam)) {
            return null;
        }
        try {
            return OrderStatus.from(statusParam);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 주문 상태 파라미터, all로 처리: {}", statusParam);
            return null;
        }
    }

    private MyOrderResponseDto toMyOrderResponseDto(Order order) {
        // 환불되지 않은 OrderItem들만 필터링
        List<OrderItemResponseDto> orderItems = order.getOrderItems().stream()
                .filter(this::isNotRefunded)
                .map(this::toOrderItemResponseDto)
                .toList();

        Integer totalAmount = getTotalAmountFromPayment(order);

        return new MyOrderResponseDto(
                order.getId(),
                order.getCreatedAt(),
                order.getTrackingNumber(),
                totalAmount,
                orderItems
        );
    }

    private boolean isNotRefunded(OrderItem orderItem) {
        // RefundItemRepository에서 해당 OrderItem이 환불되었는지 확인
        return !refundItemRepository.existsByOrderItemIdAndRefundStatusIn(
                orderItem.getId(),
                List.of(RefundStatus.APPROVED, RefundStatus.COMPLETED)
        );
    }

    private Integer getTotalAmountFromPayment(Order order) {
        return paymentRepository.findByOrderId(order.getId())
                .map(Payment::getTotalAmount)
                .orElseThrow(() -> {
                    log.error("주문 내역에 Payment가 없습니다 - 주문ID: {}", order.getId());
                    return new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
                });
    }

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

    private Integer calculateCurrentDiscountRate(GroupBuy groupBuy) {
        try {
            String discountStagesJson = groupBuy.getDiscountStages();
            List<Map<String, Object>> stages = objectMapper.readValue(
                    discountStagesJson,
                    new TypeReference<>() {}
            );

            Integer totalSalesQuantity = orderItemRepository.getTotalSalesQuantityByGroupBuyId(groupBuy.getId());

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