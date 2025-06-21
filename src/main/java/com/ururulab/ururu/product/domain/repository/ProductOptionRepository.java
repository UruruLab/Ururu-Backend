package com.ururulab.ururu.product.domain.repository;

import com.ururulab.ururu.product.domain.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    // 특정 상품의 모든 옵션 조회 (삭제되지 않은 것만)
    List<ProductOption> findByProductIdAndIsDeletedFalse(Long productId);
}
