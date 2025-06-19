package com.ururulab.ururu.product.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ProductOption")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id", nullable = false)
//    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String image_url;

    @Column(nullable = false)
    private String specification;

    @Column(nullable = false)
    private boolean is_deleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated_at;

    public static ProductOption of(
            //Product product,
            String name,
            int price,
            String image_url,
            String specification,
            boolean is_deleted
    ){
        ProductOption productOption = new ProductOption();
        productOption.name = name;
        productOption.price = price;
        productOption.image_url = image_url;
        productOption.specification = specification;
        productOption.is_deleted = is_deleted;
        return productOption;
    }
}
