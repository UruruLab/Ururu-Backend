package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.ProductNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductNoticeRepository extends JpaRepository<ProductNotice, Long> {
    // 직접 상품 ID로 조회 가능
    Optional<ProductNotice> findByProductId(Long productId);
}
