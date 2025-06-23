package com.ururulab.ururu.product.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ProductNotice")
public class ProductNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 50)
    private String capacity; //용량

    @Column(nullable = false, length = 50)
    private String spec; // 제품 주요 사양

    @Column(nullable = false, length = 100)
    private String expiry; // 사용기한

    @Column(nullable = false, columnDefinition = "TEXT")
    private String usage; //사용 방법

    @Column(nullable = false, length = 100)
    private String manufacturer; // 화장품 제조업자

    @Column(nullable = false, length = 100)
    private String responsibleSeller; // 화장품책임판매업자

    @Column(nullable = false, length = 50)
    private String countryOfOrigin; //제조국

    @Column(nullable = false)
    private boolean functionalCosmetics; // 기능성 여부

    @Column(nullable = false, columnDefinition = "TEXT")
    private String caution; // 사용 시 주의사항

    @Column(nullable = false, columnDefinition = "TEXT")
    private String warranty; // 품질 보증 기준

    @Column(nullable = false, length = 30)
    private String customerServiceNumber; //고객센터 번호

    public static ProductNotice of(
            Product product,
            String capacity,
            String spec,
            String expiry,
            String usage,
            String manufacturer,
            String responsibleSeller,
            String countryOfOrigin,
            boolean functionalCosmetics,
            String caution,
            String warranty,
            String customerServiceNumber
    ){
        ProductNotice productNotice = new ProductNotice();
        productNotice.product = product;
        productNotice.capacity = capacity;
        productNotice.spec = spec;
        productNotice.expiry = expiry;
        productNotice.usage = usage;
        productNotice.manufacturer = manufacturer;
        productNotice.responsibleSeller = responsibleSeller;
        productNotice.countryOfOrigin = countryOfOrigin;
        productNotice.functionalCosmetics = functionalCosmetics;
        productNotice.caution = caution;
        productNotice.warranty = warranty;
        productNotice.customerServiceNumber = customerServiceNumber;

        return productNotice;
    }

}
