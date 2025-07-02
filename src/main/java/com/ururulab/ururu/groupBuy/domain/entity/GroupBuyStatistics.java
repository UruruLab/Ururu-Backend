package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "GroupBuyStatistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupBuyStatistics extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupbuy_id", nullable = false)
    private GroupBuy groupBuy;

    @Column(nullable = false)
    private Integer totalParticipants; // 참여자 수

    @Column(nullable = false)
    private Integer totalQuantity; // 총 판매 수량

    @Column(nullable = false)
    private Integer finalDiscountRate; // 최종 할인율

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinalStatus finalStatus; // 최종 상태

    @Column(nullable = true)
    private Instant confirmedAt; // 확정 시각

    public static GroupBuyStatistics of(
            GroupBuy groupBuy,
            Integer totalParticipants,
            Integer totalQuantity,
            Integer finalDiscountRate,
            FinalStatus finalStatus,
            Instant confirmedAt
    ) {
        GroupBuyStatistics statistics = new GroupBuyStatistics();
        statistics.groupBuy = groupBuy;
        statistics.totalParticipants = totalParticipants;
        statistics.totalQuantity = totalQuantity;
        statistics.finalDiscountRate = finalDiscountRate;
        statistics.finalStatus = finalStatus;
        statistics.confirmedAt = confirmedAt;
        return statistics;
    }
}
