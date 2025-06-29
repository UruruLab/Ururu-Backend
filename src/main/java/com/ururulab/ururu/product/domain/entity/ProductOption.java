package com.ururulab.ururu.product.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "product_options")
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

    @Column(nullable = true)
    private String imageHash;  // 이미지 해시값 저장 (SHA-256)

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

    // 이미지 해시 업데이트 메서드
    public void updateImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    // 이미지 URL과 해시 동시 업데이트 메서드
    public void updateImageInfo(String imageUrl, String imageHash) {
        this.imageUrl = imageUrl;
        this.imageHash = imageHash;
    }

    // 이미지 정보 제거 메서드
    public void removeImageInfo() {
        this.imageUrl = null;
        this.imageHash = null;
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

    public void updateName(String name) { this.name = name; }
    public void updatePrice(Integer price) { this.price = price; }
    public void updateFullIngredients(String fullIngredients) { this.fullIngredients = fullIngredients; }

}
