package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.order.domain.policy.OrderHistoryPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(length = OrderHistoryPolicy.COMMENT_MAX_LENGTH)
    private String comment;

    public static OrderHistory create(Order order, OrderStatus status, String comment) {
        if (order == null) {
            throw new IllegalArgumentException(OrderHistoryPolicy.ORDER_REQUIRED);
        }
        if (status == null) {
            throw new IllegalArgumentException(OrderHistoryPolicy.STATUS_REQUIRED);
        }
        if (comment != null && comment.length() > OrderHistoryPolicy.COMMENT_MAX_LENGTH) {
            throw new IllegalArgumentException(OrderHistoryPolicy.COMMENT_TOO_LONG);
        }

        OrderHistory history = new OrderHistory();
        history.order = order;
        history.status = status;
        history.comment = comment;
        return history;
    }
}