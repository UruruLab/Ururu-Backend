package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    // 새로 추가: 여러 상품의 태그를 배치로 조회
    @Query("""
        SELECT pt FROM ProductTag pt 
        LEFT JOIN FETCH pt.tagCategory 
        WHERE pt.product.id IN :productIds
        ORDER BY pt.product.id, pt.id
        """)
    List<ProductTag> findByProductIdsWithTagCategory(@Param("productIds") List<Long> productIds);

    void deleteByProductIdAndTagCategoryIdIn(Long productId, Set<Long> tagCategoryIds);

    @Query("""
        SELECT pt FROM ProductTag pt 
        LEFT JOIN FETCH pt.tagCategory 
        WHERE pt.product.id = :productId
        ORDER BY pt.id
        """)
    List<ProductTag> findByProductIdWithTagCategory(@Param("productId") Long productId);
}
