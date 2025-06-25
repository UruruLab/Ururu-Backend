package com.ururulab.ururu.product.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "ProductOptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = true)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String fullIngredients; // 전성분

    public static ProductOption of(
            Product product,
            String name,
            Integer price,
            String imageUrl,
            String fullIngredients
    ){
        ProductOption productOption = new ProductOption();
        productOption.product = product;
        productOption.name = name;
        productOption.price = price;
        productOption.imageUrl = imageUrl;
        productOption.fullIngredients = fullIngredients;

        return productOption;
    }

    // 이미지 URL 업데이트 메서드 추가
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // 이미지 URL 제거 메서드 추가
    public void removeImageUrl() {
        this.imageUrl = null;
    }

    // 소프트 삭제 메서드 (필요시)
    public void markAsDeleted() {
        this.isDeleted = true;
    }

    // 소프트 삭제 복구 메서드 (필요시)
    public void restore() {
        this.isDeleted = false;
    }
}
