package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long>, GroupBuyQueryRepository {

    @Query("""
    SELECT COUNT(g) > 0
    FROM GroupBuy g
    WHERE g.product.id = :productId
      AND g.status <> 'CLOSED'
    """)
    boolean existsGroupBuyByProduct(Long productId);

    boolean existsByProductIdAndStatusNot(Long productId, GroupBuyStatus status);

    /**
     * 공동구매 상세 조회를 위한 최적화된 쿼리
     * Product와 ProductCategory만 함께 조회 (이미지는 별도)
     */
    @Query("""
    SELECT g FROM GroupBuy g
    LEFT JOIN FETCH g.product p
    LEFT JOIN FETCH p.productCategories pc
    LEFT JOIN FETCH pc.category c
    LEFT JOIN FETCH g.seller s
    WHERE g.id = :groupBuyId
    """)
    Optional<GroupBuy> findByIdWithDetails(@Param("groupBuyId") Long groupBuyId);

    /**
     * 공동구매와 이미지 정보만 함께 조회
     * MultipleBagFetchException 방지를 위해 이미지만 별도 조회
     */
    @Query("""
    SELECT g FROM GroupBuy g
    LEFT JOIN FETCH g.groupBuyImages img
    WHERE g.id = :groupBuyId
    """)
    Optional<GroupBuy> findByIdWithImages(@Param("groupBuyId") Long groupBuyId);

    /**
     * 공동구매 상세 조회 (DRAFT 제외) - 구매자용
     */
    @Query("SELECT gb FROM GroupBuy gb WHERE gb.id = :groupBuyId AND gb.status IN ('OPEN', 'CLOSED')")
    Optional<GroupBuy> findPublicGroupBuyWithDetails(@Param("groupBuyId") Long groupBuyId);

    // 카테고리별 공동구매 조회
    @Query("SELECT gb FROM GroupBuy gb " +
            "JOIN gb.product p " +
            "JOIN p.productCategories pc " +
            "WHERE pc.category.id = :categoryId AND gb.status IN ('OPEN', 'CLOSED')")
    List<GroupBuy> findByProductCategoryId(@Param("categoryId") Long categoryId);

    // 전체 공동구매 조회 (DRAFT 제외)
    @Query("SELECT gb FROM GroupBuy gb WHERE gb.status IN ('OPEN', 'CLOSED')")
    List<GroupBuy> findAllPublic();
}
