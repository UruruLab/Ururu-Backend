package com.ururulab.ururu.product.domain.entity;

import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.seller.domain.entity.Seller;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationConstants.PRODUCT_NAME_MAX;

@Entity
@Getter
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "seller_id", nullable = false)
//    private Seller seller;

    @Column(nullable = false , length = PRODUCT_NAME_MAX)
    private String name;
    @Column(nullable = false , columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> productOptions = new ArrayList<>();

    @OneToMany(mappedBy = "product", orphanRemoval = true)
    private List<ProductCategory> productCategories = new ArrayList<>();

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProductNotice productNotice;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductTag> productTags = new ArrayList<>();

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
