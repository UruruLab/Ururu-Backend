package com.ururulab.ururu.order.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "order_history")
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

    @Column(length = 255)
    private String comment;

    public static OrderHistory create(Order order, OrderStatus status, String comment) {
        if (order == null) {
            throw new IllegalArgumentException("주문 정보는 필수입니다.");
        }
        if (status == null) {
            throw new IllegalArgumentException("주문 상태는 필수입니다.");
        }

        OrderHistory history = new OrderHistory();
        history.order = order;
        history.status = status;
        history.comment = comment;
        return history;
    }
}