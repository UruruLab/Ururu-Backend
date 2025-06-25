package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.policy.OrderPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @Column(length = OrderPolicy.ID_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupbuy_id", nullable = false)
    private GroupBuy groupBuy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(length = OrderPolicy.PHONE_MAX_LENGTH, nullable = false)
    private String phone;

    @Column(length = OrderPolicy.ZONECODE_MAX_LENGTH, nullable = false)
    private String zonecode;

    @Column(length = OrderPolicy.ADDRESS_MAX_LENGTH, nullable = false)
    private String address1;

    @Column(length = OrderPolicy.ADDRESS_MAX_LENGTH)
    private String address2;

    @Column(length = OrderPolicy.TRACKING_NUMBER_MAX_LENGTH)
    private String trackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderHistory> orderHistories = new ArrayList<>();

    public static Order create(
            GroupBuy groupBuy,
            Member member,
            String phone,
            String zonecode,
            String address1,
            String address2
    ) {
        if (groupBuy == null) {
            throw new IllegalArgumentException(OrderPolicy.GROUPBUY_REQUIRED);
        }
        if (member == null) {
            throw new IllegalArgumentException(OrderPolicy.MEMBER_REQUIRED);
        }

        Order order = new Order();
        order.id = UUID.randomUUID().toString();
        order.groupBuy = groupBuy;
        order.member = member;
        order.status = OrderStatus.ORDERED;
        order.phone = phone;
        order.zonecode = zonecode;
        order.address1 = address1;
        order.address2 = address2;

        order.addOrderHistory(OrderStatus.ORDERED, OrderPolicy.ORDER_CREATION_MESSAGE);

        return order;
    }

    public void addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException(OrderPolicy.ORDER_ITEM_REQUIRED);
        }
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void addOrderHistory(OrderStatus status, String comment) {
        OrderHistory history = OrderHistory.create(this, status, comment);
        orderHistories.add(history);
    }

    public void changeStatus(OrderStatus status, String reason) {
        if (status == null) {
            throw new IllegalArgumentException(OrderPolicy.STATUS_REQUIRED);
        }
        this.status = status;
        addOrderHistory(status, reason);
    }
}