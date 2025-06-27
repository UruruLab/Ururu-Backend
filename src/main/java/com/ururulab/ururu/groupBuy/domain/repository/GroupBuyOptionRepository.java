package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupBuyOptionRepository extends JpaRepository<GroupBuyOption, Long> {

    /**
     * 공구 옵션 조회 (연관 엔티티 포함)
     * Order 도메인 장바구니 기능에서 사용:
     * - POST /cart/items: 공구 종료일 확인 (gb.endsAt)
     * - 상품 정보 확인 (p.name)
     * - 옵션 정보 확인 (po.name, po.image)
     * - 가격 정보 확인 (gbo.salePrice)
     */
    @Query("SELECT gbo FROM GroupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.groupBuy gb " +
            "LEFT JOIN FETCH gb.product p " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE gbo.id = :optionId")
    Optional<GroupBuyOption> findByIdWithDetails(@Param("optionId") Long optionId);
}