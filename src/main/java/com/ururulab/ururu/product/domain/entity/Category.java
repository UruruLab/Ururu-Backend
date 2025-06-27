package com.ururulab.ururu.product.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long parentId; // 최상위 카테고리는 NULL

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = true)
    private Integer depth;

    @Column(nullable = true)
    private Integer orderIndex;

    @Column(nullable = false, length = 500)
    private String path;

    @OneToMany(mappedBy = "category")
    private List<ProductCategory> productCategories = new ArrayList<>();

    public static Category of(
            Long parentId,
            String name,
            Integer depth,
            Integer orderIndex,
            String path
    ){
        Category category = new Category();
        category.parentId = parentId;
        category.name = name;
        category.depth = depth;
        category.orderIndex = orderIndex;
        category.path = path;

        return category;
    }
}
