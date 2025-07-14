package com.ururulab.ururu.order.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.order.dto.request.ShippingInfoUpdateRequest;
import com.ururulab.ururu.order.dto.response.ShippingInfoUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private final OrderRepository orderRepository;

    /**
     * 배송 정보를 등록합니다.
     * 판매자만 자신의 상품에 대한 주문의 배송 정보를 등록할 수 있습니다.
     *
     * @param sellerId 판매자 ID
     * @param orderId 주문 ID
     * @param request 배송 정보 등록 요청
     * @return 등록된 배송 정보
     * @throws BusinessException 주문이 존재하지 않거나, 권한이 없거나, 상태가 부적절한 경우
     */
    @Transactional
    public ShippingInfoUpdateResponse updateShippingInfo(Long sellerId, String orderId, ShippingInfoUpdateRequest request) {
        log.debug("배송 정보 등록 - 판매자ID: {}, 주문ID: {}, 운송장: {}",
                sellerId, orderId, request.trackingNumber());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        validateSellerAuthority(order, sellerId);
        validateOrderForShipping(order);

        order.updateTrackingNumber(request.trackingNumber());

        log.debug("배송 정보 등록 완료 - 주문ID: {}, 운송장: {}", orderId, request.trackingNumber());

        return new ShippingInfoUpdateResponse(
                order.getId(),
                order.getTrackingNumber()
        );
    }

    /**
     * 판매자가 해당 주문에 대한 권한이 있는지 검증합니다.
     */
    private void validateSellerAuthority(Order order, Long sellerId) {
        boolean hasAuthority = order.getOrderItems().stream()
                .anyMatch(orderItem -> {
                    Long itemSellerId = orderItem.getGroupBuyOption()
                            .getGroupBuy()
                            .getSeller()
                            .getId();
                    return itemSellerId.equals(sellerId);
                });

        if (!hasAuthority) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 주문이 배송 정보 등록 가능한 상태인지 검증합니다.
     */
    private void validateOrderForShipping(Order order) {
        if (order.getStatus() != OrderStatus.ORDERED) {
            throw new BusinessException(ErrorCode.ORDER_NOT_SHIPPABLE);
        }

        if (order.getTrackingNumber() != null) {
            throw new BusinessException(ErrorCode.TRACKING_ALREADY_REGISTERED);
        }
    }
}
