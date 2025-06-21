package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    // 특정 상품의 모든 카테고리 조회
    List<ProductCategory> findByProductId(Long productId);

    // 특정 카테고리의 모든 상품 조회
    List<ProductCategory> findByCategoryId(Long categoryId);

    // 상품-카테고리 연관관계 존재 여부 확인
    boolean existsByProductIdAndCategoryId(Long productId, Long categoryId);

    // 특정 상품의 카테고리 정보와 함께 조회
    @Query("SELECT pc FROM ProductCategory pc JOIN FETCH pc.category WHERE pc.product.id = :productId")
    List<ProductCategory> findByProductIdWithCategory(@Param("productId") Long productId);
}
