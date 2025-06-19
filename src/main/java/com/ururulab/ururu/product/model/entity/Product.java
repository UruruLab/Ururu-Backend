package com.ururulab.ururu.product.model.entity;

import com.ururulab.ururu.product.model.entity.enumerated.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "Product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //TODO 판매자 JOIN
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "seller_id", nullable = false)
//    private Seller seller;

    @Column(nullable = false , length = 255)
    private String name;
    @Column(nullable = false , columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 상품 생성날짜

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt; // 상품 수정날짜

    public static Product of(
            //Seller seller,
            String name,
            String description,
            Status status
    ) {
        Product product = new Product();
        //product.seller = seller;
        product.name = name;
        product.description = description;
        product.status = status;
        return product;
    }
}
