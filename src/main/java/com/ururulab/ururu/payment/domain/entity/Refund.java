package com.ururulab.ururu.payment.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.policy.RefundPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "refund")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(length = RefundPolicy.REASON_MAX_LENGTH, nullable = false)
    private String reason;

    @Column(nullable = false)
    private Integer amount;

    @Column
    private LocalDateTime refundedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    public static Refund create(Payment payment, String reason, Integer amount) {
        if (payment == null) {
            throw new IllegalArgumentException(RefundPolicy.PAYMENT_REQUIRED);
        }
        if (!payment.isRefundable()) {
            throw new IllegalStateException(RefundPolicy.NOT_REFUNDABLE);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException(RefundPolicy.REASON_REQUIRED);
        }
        if (amount == null) {
            throw new IllegalArgumentException(RefundPolicy.AMOUNT_REQUIRED);
        }
        if (amount < RefundPolicy.MIN_AMOUNT) {
            throw new IllegalArgumentException(RefundPolicy.AMOUNT_MIN);
        }
        if (amount > payment.getAmount()) {
            throw new IllegalArgumentException(RefundPolicy.AMOUNT_EXCEEDS_PAYMENT);
        }

        Refund refund = new Refund();
        refund.payment = payment;
        refund.reason = reason.trim();
        refund.amount = amount;
        refund.status = RefundStatus.INITIATED;

        return refund;
    }

    public void markAsCompleted(LocalDateTime refundedAt) {
        if (refundedAt == null) {
            throw new IllegalArgumentException(RefundPolicy.REFUNDED_AT_REQUIRED);
        }
        if (this.status == RefundStatus.COMPLETED) {
            throw new IllegalStateException(RefundPolicy.ALREADY_COMPLETED);
        }

        this.status = RefundStatus.COMPLETED;
        this.refundedAt = refundedAt;
    }

    public void markAsFailed() {
        if (this.status == RefundStatus.COMPLETED) {
            throw new IllegalStateException(RefundPolicy.CANNOT_FAIL_COMPLETED);
        }

        this.status = RefundStatus.FAILED;
    }

    public boolean isCompleted() {
        return this.status == RefundStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == RefundStatus.FAILED;
    }
}