package com.ururulab.ururu.product.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.ururulab.ururu.product.controller.dto.validation.ProductValidationConstants.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_notices")
public class ProductNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = CAPACITY_MAX)
    private String capacity; //용량

    @Column(nullable = false, length = SPEC_MAX)
    private String spec; // 제품 주요 사양

    @Column(nullable = false, length = EXPIRY_MAX)
    private String expiry; // 사용기한

    @Column(name = "usage_guide", nullable = false, columnDefinition = "TEXT")
    private String usage; //사용 방법 // 컬럼명: usage_guide (MySQL 예약어 회피용)

    @Column(nullable = false, length = MANUFACTURER_MAX)
    private String manufacturer; // 화장품 제조업자

    @Column(nullable = false, length = RESPONSIBLE_SELLER_MAX)
    private String responsibleSeller; // 화장품책임판매업자

    @Column(nullable = false, length = COUNTRY_OF_ORIGIN_MAX)
    private String countryOfOrigin; //제조국

    @Column(nullable = false)
    private Boolean functionalCosmetics; // 기능성 여부

    @Column(nullable = false, columnDefinition = "TEXT")
    private String caution; // 사용 시 주의사항

    @Column(nullable = false, columnDefinition = "TEXT")
    private String warranty; // 품질 보증 기준

    @Column(nullable = false, length = CUSTOMER_SERVICE_NUMBER_MAX)
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
            Boolean functionalCosmetics,
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

    public void updateCapacity(String capacity) { this.capacity = capacity; }
    public void updateSpec(String spec) { this.spec = spec; }
    public void updateExpiry(String expiry) { this.expiry = expiry; }
    public void updateUsage(String usage) { this.usage = usage; }
    public void updateManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void updateResponsibleSeller(String responsibleSeller) { this.responsibleSeller = responsibleSeller; }
    public void updateCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }
    public void updateFunctionalCosmetics(Boolean functionalCosmetics) { this.functionalCosmetics = functionalCosmetics; }
    public void updateCaution(String caution) { this.caution = caution; }
    public void updateWarranty(String warranty) { this.warranty = warranty; }
    public void updateCustomerServiceNumber(String customerServiceNumber) { this.customerServiceNumber = customerServiceNumber; }
}
