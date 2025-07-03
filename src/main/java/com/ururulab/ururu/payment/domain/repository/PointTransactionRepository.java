package com.ururulab.ururu.payment.domain.repository;

import com.ururulab.ururu.payment.domain.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /**
     * 회원별 포인트 이력 조회 (최신순)
     * 마이페이지 포인트 내역에서 사용
     */
    @Query("SELECT pt FROM PointTransaction pt " +
            "WHERE pt.member.id = :memberId " +
            "ORDER BY pt.createdAt DESC")
    List<PointTransaction> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);
}