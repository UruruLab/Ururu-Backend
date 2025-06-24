package com.ururulab.ururu.groupBuy.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import jakarta.persistence.*;
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
    private Integer stock;

    @Column(nullable = false)
    private Integer priceOverride;

    @Column(nullable = false)
    private Integer salePrice;

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
