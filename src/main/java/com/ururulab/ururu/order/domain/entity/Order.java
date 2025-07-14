package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.policy.OrderPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @Column(length = OrderPolicy.ID_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(length = OrderPolicy.PHONE_MAX_LENGTH)
    private String phone;

    @Column(length = OrderPolicy.ZONECODE_MAX_LENGTH)
    private String zonecode;

    @Column(length = OrderPolicy.ADDRESS_MAX_LENGTH)
    private String address1;

    @Column(length = OrderPolicy.ADDRESS_MAX_LENGTH)
    private String address2;

    @Column(length = OrderPolicy.TRACKING_NUMBER_MAX_LENGTH)
    private String trackingNumber;

    @Column
    private Instant trackingRegisteredAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderHistory> orderHistories = new ArrayList<>();

    public static Order create(Member member) {
        if (member == null) {
            throw new IllegalArgumentException(OrderPolicy.MEMBER_REQUIRED);
        }

        Order order = new Order();
        order.id = UUID.randomUUID().toString();
        order.member = member;
        order.status = OrderStatus.PENDING;
        order.phone = null;
        order.zonecode = null;
        order.address1 = null;
        order.address2 = null;

        order.addOrderHistory(OrderStatus.PENDING, OrderPolicy.ORDER_CREATION_MESSAGE);

        return order;
    }

    public void completePaymentInfo(String phone, String zonecode, String address1, String address2) {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(OrderPolicy.PAYMENT_INFO_ONLY_PENDING);
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException(OrderPolicy.PHONE_REQUIRED);
        }
        if (phone.length() > OrderPolicy.PHONE_MAX_LENGTH) {
            throw new IllegalArgumentException(OrderPolicy.PHONE_TOO_LONG);
        }
        if (zonecode == null || zonecode.trim().isEmpty()) {
            throw new IllegalArgumentException(OrderPolicy.ZONECODE_REQUIRED);
        }
        if (zonecode.length() > OrderPolicy.ZONECODE_MAX_LENGTH) {
            throw new IllegalArgumentException(OrderPolicy.ZONECODE_TOO_LONG);
        }
        if (address1 == null || address1.trim().isEmpty()) {
            throw new IllegalArgumentException(OrderPolicy.ADDRESS1_REQUIRED);
        }
        if (address1.length() > OrderPolicy.ADDRESS_MAX_LENGTH) {
            throw new IllegalArgumentException(OrderPolicy.ADDRESS1_TOO_LONG);
        }
        if (address2 != null && address2.length() > OrderPolicy.ADDRESS_MAX_LENGTH) {
            throw new IllegalArgumentException(OrderPolicy.ADDRESS2_TOO_LONG);
        }

        this.phone = phone.trim();
        this.zonecode = zonecode.trim();
        this.address1 = address1.trim();
        this.address2 = address2.trim();

        this.addOrderHistory(OrderStatus.PENDING, OrderPolicy.PAYMENT_INFO_COMPLETED);
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

    public void updateTrackingNumber(String trackingNumber) {

        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(OrderPolicy.TRACKING_NUMBER_REQUIRED);
        }
        if (trackingNumber.trim().length() > OrderPolicy.TRACKING_NUMBER_MAX_LENGTH) {
            throw new IllegalArgumentException(OrderPolicy.TRACKING_NUMBER_TOO_LONG);
        }
        this.trackingNumber = trackingNumber;
        this.trackingRegisteredAt = Instant.now();
    }
}