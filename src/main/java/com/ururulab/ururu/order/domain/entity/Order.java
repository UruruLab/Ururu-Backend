package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.entity.enumerated.PaymentStatus;
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
    @Column(length = 36)
    private String id;

    // TODO: GroupBuy 엔티티 완성 후 연관관계로 변경
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "groupbuy_id", nullable = false)
    // private GroupBuy groupBuy;

    @Column(name = "groupbuy_id", nullable = false)
    private Long groupBuyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(length = 5, nullable = false)
    private String zonecode;

    @Column(length = 255, nullable = false)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(length = 50)
    private String trackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderHistory> orderHistories = new ArrayList<>();

    public static Order create(
            Long groupBuyId,
            Member member,
            String phone,
            String zonecode,
            String address1,
            String address2
    ) {
        if (groupBuyId == null) {
            throw new IllegalArgumentException("공동구매 ID는 필수입니다.");
        }
        if (member == null) {
            throw new IllegalArgumentException("회원 정보는 필수입니다.");
        }

        Order order = new Order();
        order.id = UUID.randomUUID().toString();
        order.groupBuyId = groupBuyId;
        order.member = member;
        order.status = OrderStatus.ORDERED;
        order.paymentStatus = PaymentStatus.PENDING;
        order.phone = phone;
        order.zonecode = zonecode;
        order.address1 = address1;
        order.address2 = address2;

        order.addOrderHistory(OrderStatus.ORDERED, "주문이 생성되었습니다.");

        return order;
    }

    public void addOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException("주문 아이템은 필수입니다.");
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
            throw new IllegalArgumentException("주문 상태는 필수입니다.");
        }
        this.status = status;
        addOrderHistory(status, reason);
    }

    public void changePaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            throw new IllegalArgumentException("결제 상태는 필수입니다.");
        }
        this.paymentStatus = paymentStatus;
    }
}