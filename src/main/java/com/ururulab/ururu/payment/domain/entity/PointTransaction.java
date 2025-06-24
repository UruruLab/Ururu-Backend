package com.ururulab.ururu.payment.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointType;
import com.ururulab.ururu.payment.domain.policy.PointTransactionPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "point_transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointSource source;

    @Column(nullable = false)
    private Integer amount;

    @Column(length = PointTransactionPolicy.REASON_MAX_LENGTH)
    private String reason;

    public static PointTransaction createEarned(
            Member member,
            PointSource source,
            Integer amount,
            String reason
    ) {
        return create(member, PointType.EARNED, source, amount, reason);
    }

    public static PointTransaction createUsed(
            Member member,
            PointSource source,
            Integer amount,
            String reason
    ) {
        return create(member, PointType.USED, source, amount, reason);
    }

    private static PointTransaction create(
            Member member,
            PointType type,
            PointSource source,
            Integer amount,
            String reason
    ) {
        validateCommonParameters(member, type, source, amount);
        validateTypeSpecific(type, amount);

        PointTransaction transaction = new PointTransaction();
        transaction.member = member;
        transaction.type = type;
        transaction.source = source;
        transaction.amount = amount;
        transaction.reason = reason != null ? reason.trim() : null;

        return transaction;
    }

    private static void validateCommonParameters(Member member, PointType type, PointSource source, Integer amount) {
        if (member == null) {
            throw new IllegalArgumentException(PointTransactionPolicy.MEMBER_REQUIRED);
        }
        if (type == null) {
            throw new IllegalArgumentException(PointTransactionPolicy.TYPE_REQUIRED);
        }
        if (source == null) {
            throw new IllegalArgumentException(PointTransactionPolicy.SOURCE_REQUIRED);
        }
        if (amount == null) {
            throw new IllegalArgumentException(PointTransactionPolicy.AMOUNT_REQUIRED);
        }
    }

    private static void validateTypeSpecific(PointType type, Integer amount) {
        if (type == PointType.EARNED) {
            if (amount <= PointTransactionPolicy.MIN_EARNED_AMOUNT) {
                throw new IllegalArgumentException(PointTransactionPolicy.EARNED_AMOUNT_MIN);
            }
        } else if (type == PointType.USED) {
            if (amount <= PointTransactionPolicy.MIN_USED_AMOUNT) {
                throw new IllegalArgumentException(PointTransactionPolicy.USED_AMOUNT_MIN);
            }
        }
    }

    public boolean isEarned() {
        return this.type == PointType.EARNED;
    }

    public boolean isUsed() {
        return this.type == PointType.USED;
    }
}