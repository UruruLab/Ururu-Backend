package com.ururulab.ururu.payment.domain.repository;

import com.ururulab.ururu.payment.domain.entity.PointTransaction;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /**
     * 회원별 포인트 거래내역 조회 (페이징, 필터링)
     * type이나 source가 null이면 해당 조건 무시
     */
    @Query("SELECT pt FROM PointTransaction pt " +
            "WHERE pt.member.id = :memberId " +
            "AND (:type IS NULL OR pt.type = :type) " +
            "AND (:source IS NULL OR pt.source = :source)")
    Page<PointTransaction> findByMemberIdWithFilters(
            @Param("memberId") Long memberId,
            @Param("type") PointType type,
            @Param("source") PointSource source,
            Pageable pageable
    );

    int countByMemberId(Long memberId);
}