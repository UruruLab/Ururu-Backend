package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.order.domain.policy.OrderItemPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupbuy_option_id", nullable = false)
    private GroupBuyOption groupBuyOption;

    @Column(nullable = false)
    private int quantity;

    public static OrderItem create(GroupBuyOption groupBuyOption, int quantity) {
        if (groupBuyOption == null) {
            throw new IllegalArgumentException(OrderItemPolicy.GROUPBUY_OPTION_REQUIRED);
        }
        if (quantity < OrderItemPolicy.MIN_QUANTITY) {
            throw new IllegalArgumentException(OrderItemPolicy.QUANTITY_MIN);
        }

        OrderItem orderItem = new OrderItem();
        orderItem.groupBuyOption = groupBuyOption;
        orderItem.quantity = quantity;
        return orderItem;
    }

    public void assignOrder(Order order) {
        this.order = order;
    }
}