package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "GroupBuyImage")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupBuyImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupbuy_id", nullable = false)
    private GroupBuy groupBuy;

    @Column(nullable = false)
    private String imageUrl; // 상세 페이지 이미지

    @Column(nullable = false)
    private Integer displayOrder; // 이미지 순서

    @Column(nullable = false)
    private Boolean isDeleted; // 삭제 여부

    public static GroupBuyImage of(
            GroupBuy groupBuy,
            String imageUrl,
            Integer displayOrder,
            Boolean isDeleted
    ) {
        GroupBuyImage groupBuyImage = new GroupBuyImage();
        groupBuyImage.groupBuy = groupBuy;
        groupBuyImage.imageUrl = imageUrl;
        groupBuyImage.displayOrder = displayOrder;
        groupBuyImage.isDeleted = isDeleted;
        return groupBuyImage;
    }
}
