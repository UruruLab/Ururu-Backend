package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface GroupBuyRepository extends JpaRepository<GroupBuy, Long> {

    @Query("""
    SELECT COUNT(g) > 0
    FROM GroupBuy g
    WHERE g.product.id = :productId
      AND g.status <> 'CLOSED'
    """)
    boolean existsGroupBuyByProduct(Long productId);

    boolean existsByProductIdAndStatusNot(Long productId, GroupBuyStatus status);
}
