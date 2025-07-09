package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.seller.domain.entity.Seller;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationConstants.*;

@Entity
@Getter
@Table(name = "groupbuys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupBuy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false, length = GROUP_BUY_TITLE_MAX)
    private String title; // 공동구매 타이틀

    @Column(columnDefinition = "TEXT")
    private String description; // 공동구매 상세설명

    @Column(nullable = true, columnDefinition = "TEXT")
    private String thumbnailUrl; // 대표 이미지

    @Column(nullable = true)
    private String thumbnailHash;  // 이미지 해시값 저장 (SHA-256)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private String discountStages; // 달성 인원에 따른 할인율

    @Column(nullable = false)
    private Integer limitQuantityPerMember; // 1인 최대 수량 제한

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupBuyStatus status; // 공동구매 상태

    /**
     * 화면 표시용 최종 할인 적용 가격 (성능 최적화를 위한 비정규화)
     * = 첫 번째 옵션의 priceOverride * (100 - 최고 할인율) / 100
     */
    @Column(name = "display_final_price")
    private Integer displayFinalPrice;

    @Column(nullable = false)
    private Instant startAt; // 공동구매 시작일

    @Column(nullable = false)
    private Instant endsAt; // 공동구매 종료일

    @OneToMany(mappedBy = "groupBuy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupBuyImage> groupBuyImages = new ArrayList<>();

    public static GroupBuy of(
            Product product,
            Seller seller,
            String title,
            String description,
            String thumbnailUrl,
            String discountStages,
            Integer limitQuantityPerMember,
            GroupBuyStatus status,
            Instant startAt,
            Instant endsAt
    ) {
        GroupBuy groupBuy = new GroupBuy();
        groupBuy.product = product;
        groupBuy.seller = seller;
        groupBuy.title = title;
        groupBuy.description = description;
        groupBuy.thumbnailUrl = thumbnailUrl;
        groupBuy.discountStages = discountStages;
        groupBuy.limitQuantityPerMember = limitQuantityPerMember;
        groupBuy.displayFinalPrice = null; // 등록 이후에 계산
        groupBuy.status = status;
        groupBuy.startAt = startAt;
        groupBuy.endsAt = endsAt;
        return groupBuy;
    }

    public void updateThumbnailInfo(String thumbnailUrl, String thumbnailHash) {
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailHash = thumbnailHash;
    }

    public void updateStatus(GroupBuyStatus status) {
        this.status = status;
    }

    /**
     * 화면 표시용 최종 가격 업데이트
     */
    public void updateDisplayFinalPrice(Integer finalPrice) {
        this.displayFinalPrice = finalPrice;
    }
}
