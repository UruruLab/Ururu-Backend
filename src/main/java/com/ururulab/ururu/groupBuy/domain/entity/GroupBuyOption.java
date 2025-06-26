package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "GroupBuyOption")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupBuyOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupbuy_id", nullable = false)
    private GroupBuy groupBuy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    @Column(nullable = false)
    @Min(0)
    private Integer stock; //재고

    @Column(nullable = false)
    @Min(0)
    private Integer priceOverride; // 공구 시작가

    @Column(nullable = false)
    @Min(0)
    private Integer salePrice; // 실제 판매가

    public static GroupBuyOption of(
            GroupBuy groupBuy,
            ProductOption productOption,
            Integer stock,
            Integer priceOverride,
            Integer salePrice
    ) {
        GroupBuyOption groupBuyOption = new GroupBuyOption();
        groupBuyOption.groupBuy = groupBuy;
        groupBuyOption.productOption = productOption;
        groupBuyOption.stock = stock;
        groupBuyOption.priceOverride = priceOverride;
        groupBuyOption.salePrice = salePrice;
        return groupBuyOption;
    }
}
