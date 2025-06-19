package com.ururulab.ururu.domain.product.model.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ProductNotice")
public class ProductNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

    @Column(nullable = false, length = 50)
    private String capacity; //용량

    @Column(nullable = false, length = 100)
    private String expiry; // 사용기한

    @Column(nullable = false, columnDefinition = "TEXT")
    private String usage; //사용 방법

    @Column(nullable = false, length = 100)
    private String manufacturer; // 화장품 제조업자

    @Column(nullable = false, length = 100)
    private String responsible_seller; // 화장품책임판매업자

    @Column(nullable = false, length = 50)
    private String country_of_origin; //제조국

    @Column(nullable = false, columnDefinition = "TEXT")
    private String full_ingredients; // 전성분

    @Column(nullable = false)
    private boolean functional_cosmetics; // 기능성 여부

    @Column(nullable = false, columnDefinition = "TEXT")
    private String caution; // 사용 시 주의사항

    @Column(nullable = false, columnDefinition = "TEXT")
    private String warranty; // 품질 보증 기준

    @Column(nullable = false, length = 30)
    private String customer_service_number; //고객센터 번호

    public static ProductNotice of(
            ProductOption productOption,
            String capacity,
            String expiry,
            String usage,
            String manufacturer,
            String responsible_seller,
            String country_of_origin,
            String full_ingredients,
            boolean functional_cosmetics,
            String caution,
            String warranty,
            String customer_service_number
    ){
        ProductNotice productNotice = new ProductNotice();
        productNotice.productOption = productOption;
        productNotice.capacity = capacity;
        productNotice.expiry = expiry;
        productNotice.usage = usage;
        productNotice.manufacturer = manufacturer;
        productNotice.responsible_seller = responsible_seller;
        productNotice.country_of_origin = country_of_origin;
        productNotice.full_ingredients = full_ingredients;
        productNotice.functional_cosmetics = functional_cosmetics;
        productNotice.caution = caution;
        productNotice.warranty = warranty;
        productNotice.customer_service_number = customer_service_number;

        return productNotice;
    }

}
