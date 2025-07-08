package com.ururulab.ururu.payment.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.order.domain.entity.OrderItem;
import com.ururulab.ururu.payment.domain.policy.RefundItemPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "refund_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    public static RefundItem create(OrderItem orderItem) {
        if (orderItem == null) {
            throw new IllegalArgumentException(RefundItemPolicy.ORDER_ITEM_REQUIRED);
        }

        RefundItem refundItem = new RefundItem();
        refundItem.orderItem = orderItem;
        return refundItem;
    }

    public void assignRefund(Refund refund) {
        if (refund == null) {
            throw new IllegalArgumentException(RefundItemPolicy.REFUND_REQUIRED);
        }
        this.refund = refund;
    }
}