package com.ururulab.ururu.payment.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.order.domain.policy.OrderPolicy;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;
import com.ururulab.ururu.payment.domain.policy.RefundPolicy;
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
@Table(name = "refund")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @Column(length = RefundPolicy.ID_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundType type;

    @Column(length = RefundPolicy.REASON_MAX_LENGTH, nullable = false)
    private String reason;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(length = RefundPolicy.REJECT_REASON_MAX_LENGTH)
    private String rejectReason;

    @Column
    private Instant refundedAt;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundItem> refundItems = new ArrayList<>();

    public static Refund create(Payment payment, RefundType type, String reason, Integer amount) {
        if (payment == null) {
            throw new IllegalArgumentException(RefundPolicy.PAYMENT_REQUIRED);
        }
        if (!payment.isRefundable()) {
            throw new IllegalStateException(RefundPolicy.NOT_REFUNDABLE);
        }
        if (type == null) {
            throw new IllegalArgumentException(RefundPolicy.TYPE_REQUIRED);
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
        refund.id = UUID.randomUUID().toString();
        refund.payment = payment;
        refund.type = type;
        refund.reason = reason.trim();
        refund.amount = amount;
        refund.status = RefundStatus.INITIATED;

        return refund;
    }

    public void addRefundItem(RefundItem refundItem) {
        if (refundItem == null) {
            throw new IllegalArgumentException(RefundPolicy.REFUND_ITEM_REQUIRED);
        }
        refundItems.add(refundItem);
        refundItem.assignRefund(this);
    }

    public void markAsApproved() {
        if (this.status != RefundStatus.INITIATED) {
            throw new IllegalStateException(RefundPolicy.INVALID_STATUS_FOR_APPROVAL);
        }
        this.status = RefundStatus.APPROVED;
    }

    public void markAsRejected(String rejectReason) {
        if (this.status != RefundStatus.INITIATED) {
            throw new IllegalStateException(RefundPolicy.INVALID_STATUS_FOR_REJECTION);
        }
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new IllegalArgumentException(RefundPolicy.REJECT_REASON_REQUIRED);
        }
        this.status = RefundStatus.REJECTED;
        this.rejectReason = rejectReason.trim();
    }

    public void markAsCompleted(Instant refundedAt) {
        if (this.status != RefundStatus.APPROVED) {
            throw new IllegalStateException(RefundPolicy.INVALID_STATUS_FOR_COMPLETION);
        }
        if (refundedAt == null) {
            throw new IllegalArgumentException(RefundPolicy.REFUNDED_AT_REQUIRED);
        }
        this.status = RefundStatus.COMPLETED;
        this.refundedAt = refundedAt;
    }

    public void markAsFailed() {
        if (this.status != RefundStatus.APPROVED) {
            throw new IllegalStateException(RefundPolicy.INVALID_STATUS_FOR_FAILURE);
        }
        this.status = RefundStatus.FAILED;
    }

    public boolean isInitiated() {
        return this.status == RefundStatus.INITIATED;
    }

    public boolean isApproved() {
        return this.status == RefundStatus.APPROVED;
    }

    public boolean isRejected() {
        return this.status == RefundStatus.REJECTED;
    }

    public boolean isCompleted() {
        return this.status == RefundStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == RefundStatus.FAILED;
    }
}