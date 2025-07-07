package com.ururulab.ururu.payment.domain.repository;

import com.ururulab.ururu.payment.domain.entity.Refund;
import com.ururulab.ururu.payment.domain.entity.RefundItem;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    /**
     * 특정 OrderItem이 특정 상태로 환불되었는지 확인
     */
    @Query("SELECT COUNT(ri) > 0 FROM RefundItem ri " +
            "JOIN ri.refund r " +
            "WHERE ri.orderItem.id = :orderItemId " +
            "AND r.status IN :statuses")
    boolean existsByOrderItemIdAndRefundStatusIn(
            @Param("orderItemId") Long orderItemId,
            @Param("statuses") List<RefundStatus> statuses
    );
}