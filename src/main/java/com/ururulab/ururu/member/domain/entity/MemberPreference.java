package com.ururulab.ururu.member.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "MemberPreference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPreference extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    //TODO 판매자 JOIN - Seller 엔티티 구현 후 추가
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "seller_id", nullable = false)
    // private Seller seller;
    private Long sellerId; // 임시로 Long 타입으로 처리

    @Column(nullable = false)
    @Min(value = 1, message = "선호도 레벨은 1 이상이어야 합니다")
    @Max(value = 5, message = "선호도 레벨은 5 이하여야 합니다")
    private int preferenceLevel; // 1~5

    private int monthlyBudget;

    @Column(length = 50)
    private String preferredPriceRange;

    @Enumerated(EnumType.STRING)
    private PurchaseFrequency purchaseFrequency;

    public static MemberPreference of(
            Member member,
            Long sellerId,
            int preferenceLevel,
            int monthlyBudget,
            String preferredPriceRange,
            PurchaseFrequency purchaseFrequency
    ) {
        MemberPreference memberPreference = new MemberPreference();
        memberPreference.member = member;
        memberPreference.sellerId = sellerId;
        memberPreference.preferenceLevel = preferenceLevel;
        memberPreference.monthlyBudget = monthlyBudget;
        memberPreference.preferredPriceRange = preferredPriceRange;
        memberPreference.purchaseFrequency = purchaseFrequency;
        return memberPreference;
    }
}
