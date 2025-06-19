package com.ururulab.ururu.domain.product.model.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Getter
@Table(name = "ProductOption")
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
    private int price;

    @Column(nullable = false)
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "JSON")
    private Map<String, String> specifications;

    @Column(nullable = false)
    private boolean isDeleted = false;

    public static ProductOption of(
            Product product,
            String name,
            int price,
            String imageUrl,
            Map<String,String> specifications,
            boolean isDeleted
    ){
        ProductOption productOption = new ProductOption();
        productOption.product = product;
        productOption.name = name;
        productOption.price = price;
        productOption.imageUrl = imageUrl;
        productOption.specifications = specifications;
        productOption.isDeleted = isDeleted;
        return productOption;
    }
}
