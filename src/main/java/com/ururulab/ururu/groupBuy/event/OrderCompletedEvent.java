package com.ururulab.ururu.groupBuy.event;

import com.ururulab.ururu.order.domain.entity.OrderItem;

import java.util.List;

public record OrderCompletedEvent(
        List<OrderItemInfo> orderItems
) {
    /**
     * 주문 아이템 정보 - 재고 체크에 필요한 최소 정보만 포함
     */
    public record OrderItemInfo(
            Long orderItemId,
            Long groupBuyOptionId,
            Long groupBuyId,
            Integer quantity
    ) {}

    /**
     * OrderItem List로부터 이벤트 생성
     * @param orderItems 완료된 주문의 OrderItem 리스트
     * @return OrderCompletedEvent
     */
    public static OrderCompletedEvent from(List<OrderItem> orderItems) {
        List<OrderItemInfo> orderItemInfos = orderItems.stream()
                .map(orderItem -> new OrderItemInfo(
                        orderItem.getId(),
                        orderItem.getGroupBuyOption().getId(),
                        orderItem.getGroupBuyOption().getGroupBuy().getId(),
                        orderItem.getQuantity()
                ))
                .toList();

        return new OrderCompletedEvent(orderItemInfos);
    }

    /**
     * 단일 OrderItem으로부터 이벤트 생성
     * @param orderItem 완료된 OrderItem
     * @return OrderCompletedEvent
     */
    public static OrderCompletedEvent from(OrderItem orderItem) {
        return from(List.of(orderItem));
    }

    /**
     * 간단한 팩토리 메서드 - 직접 정보 제공
     * @param orderItemInfos 주문 아이템 정보 리스트
     * @return OrderCompletedEvent
     */
    public static OrderCompletedEvent of(List<OrderItemInfo> orderItemInfos) {
        return new OrderCompletedEvent(orderItemInfos);
    }
}
