package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.FinalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private Integer totalParticipants;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer finalDiscountRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinalStatus finalStatus;

    @Column(nullable = true)
    private LocalDateTime confirmedAt;

    public static GroupBuyStatistics of(
            GroupBuy groupBuy,
            Integer totalParticipants,
            Integer totalQuantity,
            Integer finalDiscountRate,
            FinalStatus finalStatus,
            LocalDateTime confirmedAt
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
