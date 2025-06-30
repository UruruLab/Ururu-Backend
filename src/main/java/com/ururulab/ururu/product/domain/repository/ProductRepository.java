package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 판매자의 모든 상품 조회 (삭제되지 않은 것만)
    // TODO: 판매자 연관관계 추가 후 수정 필요
    // List<Product> findBySellerIdAndStatusNot(Long sellerId, Status status);

    // 판매자의 활성화된 상품만 조회 (공동구매 등록용)
    // TODO: 판매자 연관관계 추가 후 수정 필요
    // List<Product> findBySellerIdAndStatus(Long sellerId, Status status);

    /**
     * 특정 판매자의 상품 목록 조회 (페이징)
     */
    @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId AND p.status IN :statuses ORDER BY p.createdAt DESC, p.id DESC")
    Page<Product> findBySellerIdAndStatusIn(
            @Param("sellerId") Long sellerId,
            @Param("statuses") List<Status> statuses,
            Pageable pageable
    );


    // 임시: 상품 상태별 조회 (판매자 연관관계 추가 전까지 사용)
    Page<Product> findByStatusNot(Status status, Pageable pageable);

    // 상품 ID와 상태로 조회
    Optional<Product> findByIdAndStatus(Long id, Status status);

    // 상품 + 옵션들 함께 조회
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productOptions po WHERE p.id = :productId AND po.isDeleted = false")
    Optional<Product> findByIdWithOptions(@Param("productId") Long productId);

    //상품 + 정보고시 함께 조회 -> 엔티티 구조 변경 후 주석 제거
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productNotice WHERE p.id = :productId")
    Optional<Product> findByIdWithNotice(@Param("productId") Long productId);

    @Query("SELECT p FROM Product p WHERE p.status IN :statuses ORDER BY p.createdAt DESC, p.id DESC")
    Page<Product> findByStatusIn(@Param("statuses") List<Status> statuses, Pageable pageable);

    // 상세 조회용: 특정 판매자의 특정 상품 조회 (sellerId 기반)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.seller.id = :sellerId AND p.status IN :statuses")
    Optional<Product> findByIdAndSellerIdAndStatusIn(
            @Param("id") Long id,
            @Param("sellerId") Long sellerId,
            @Param("statuses") List<Status> statuses);
}
