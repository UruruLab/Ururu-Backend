package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * 공동구매 상세 조회 (DRAFT, CLOSED 제외) - 구매자용
     */
    @Query("SELECT gb FROM GroupBuy gb WHERE gb.id = :groupBuyId AND gb.status ='OPEN'")
    Optional<GroupBuy> findPublicGroupBuyWithDetails(@Param("groupBuyId") Long groupBuyId);

    /**
     * 만료된 공동구매 조회 (OPEN 상태이면서 종료일이 지난 것들)
     * 배치 처리용 - 필요한 연관 엔티티들을 한번에 페치
     */
    @Query("""
        SELECT DISTINCT gb FROM GroupBuy gb
        LEFT JOIN FETCH gb.product p
        LEFT JOIN FETCH gb.seller s
        LEFT JOIN FETCH gb.options o
        WHERE gb.status = 'OPEN' 
        AND gb.endsAt <= :currentTime
        ORDER BY gb.endsAt ASC
        """)
    List<GroupBuy> findExpiredGroupBuys(@Param("currentTime") Instant currentTime);

    @Query("""
    SELECT DISTINCT gb FROM GroupBuy gb
    LEFT JOIN FETCH gb.options gbo
    LEFT JOIN FETCH gbo.productOption po
    JOIN gb.product p
    JOIN p.productCategories pc
    WHERE (:categoryId IS NULL OR pc.category.id = :categoryId)
      AND gb.status = 'OPEN'
      AND gb.endsAt > CURRENT_TIMESTAMP
    """)
    List<GroupBuy> findByProductCategoryIdWithOptions(@Param("categoryId") Long categoryId);

    /**
     * 전체 공개 공동구매 조회 (옵션 정보 포함)
     * N+1 쿼리 방지를 위해 FETCH JOIN 사용
     */
    @Query("""
        SELECT DISTINCT gb FROM GroupBuy gb
        LEFT JOIN FETCH gb.options gbo
        LEFT JOIN FETCH gbo.productOption po
        WHERE gb.status = 'OPEN'
          AND gb.endsAt > CURRENT_TIMESTAMP
        """)
    List<GroupBuy> findAllPublicWithOptions();


    @Query("SELECT gb FROM GroupBuy gb WHERE gb.seller.id = :sellerId ORDER BY gb.createdAt DESC")
    Page<GroupBuy> findBySellerIdWithPagination(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("SELECT gb FROM GroupBuy gb JOIN FETCH gb.options WHERE gb.id IN :groupBuyIds")
    List<GroupBuy> findByIdsWithOptions(@Param("groupBuyIds") List<Long> groupBuyIds);
}
