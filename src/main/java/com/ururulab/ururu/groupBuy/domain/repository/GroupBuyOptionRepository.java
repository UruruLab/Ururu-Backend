package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * 공구 옵션 재고 차감
     * Payment 도메인에서 결제 완료 시 사용:
     * - 결제 승인 완료 후 실제 재고 차감
     * - 동시성 안전을 위한 낙관적 업데이트 (재고가 충분할 때만 차감)
     * - 차감 실패 시 0 반환하여 재고 부족 상황 감지
     */
    @Modifying
    @Query("UPDATE GroupBuyOption gbo SET gbo.stock = gbo.stock - :quantity " +
            "WHERE gbo.id = :optionId AND gbo.stock >= :quantity")
    int decreaseStock(@Param("optionId") Long optionId, @Param("quantity") Integer quantity);

    /**
     * 공동구매별 옵션 조회 (ProductOption 정보 포함)
     * 상세 페이지에서 사용 - 옵션의 이름, 가격, 이미지 등이 필요
     */
    @Query("SELECT gbo FROM GroupBuyOption gbo " +
            "LEFT JOIN FETCH gbo.productOption po " +
            "WHERE gbo.groupBuy = :groupBuy " +
            "ORDER BY po.id ASC")
    List<GroupBuyOption> findAllByGroupBuy(GroupBuy groupBuy);

    /**
     * 공동구매별 옵션 조회 (기본 - ProductOption 페치 없음)
     * 단순 재고 조회 등에 사용
     */
    @Query("SELECT gbo FROM GroupBuyOption gbo " +
            "WHERE gbo.groupBuy = :groupBuy " +
            "ORDER BY gbo.id ASC")
    List<GroupBuyOption> findAllByGroupBuyBasic(@Param("groupBuy") GroupBuy groupBuy);
}