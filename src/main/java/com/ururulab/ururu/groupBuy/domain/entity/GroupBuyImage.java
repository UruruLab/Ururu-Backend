package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "GroupBuyImages")
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

    @Column(nullable = true)
    private String detailImageHash;  // 이미지 해시값 저장 (SHA-256)

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
