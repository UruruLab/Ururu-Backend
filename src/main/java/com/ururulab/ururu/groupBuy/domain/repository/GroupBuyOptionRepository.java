package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.dto.projection.GroupBuyOptionBasicInfo;
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
     * 여러 옵션 ID로 옵션 ID와 이름만 조회
     * @param ids
     * @return
     */
    @Query("SELECT gbo.id as id, gbo.productOption.name as name, gbo.productOption.imageUrl as imageUrl FROM GroupBuyOption gbo WHERE gbo.id IN :ids")
    List<GroupBuyOptionBasicInfo> findIdAndNameByIdIn(@Param("ids") List<Long> ids);

    /**
     * 지정된 공동구매 ID에 해당하는 모든 공동구매 옵션을 삭제
     * @param groupBuyId
     */
    void deleteAllByGroupBuyId(Long groupBuyId);

    /**
     * 특정 공동구매의 총 판매량 조회
     * - OrderItem 집계 대신 사용
     *
     * @param groupBuyId 공동구매 ID
     * @return 총 판매량 (모든 옵션의 판매량 합계)
     */
    @Query("SELECT COALESCE(SUM(gbo.initialStock - gbo.stock), 0) " +
            "FROM GroupBuyOption gbo " +
            "WHERE gbo.groupBuy.id = :groupBuyId")
    Integer getTotalSoldQuantityByGroupBuyId(@Param("groupBuyId") Long groupBuyId);

    /**
     * 여러 공동구매의 총 판매량 조회
     * - 공동구매 목록 조회 시 사용
     *
     * @param groupBuyIds 공동구매 ID 리스트
     * @return [groupBuyId, totalSoldQuantity] 형태의 결과 리스트
     */
    @Query("SELECT gbo.groupBuy.id, COALESCE(SUM(gbo.initialStock - gbo.stock), 0) " +
            "FROM GroupBuyOption gbo " +
            "WHERE gbo.groupBuy.id IN :groupBuyIds " +
            "GROUP BY gbo.groupBuy.id")
    List<Object[]> getTotalSoldQuantitiesByGroupBuyIds(@Param("groupBuyIds") List<Long> groupBuyIds);


    /**
     * 특정 공동구매의 모든 재고가 소진되었는지 확인
     * - 기존 복잡한 OrderItem 집계 제거
     * - 단순한 stock 필드 확인
     *
     * @param groupBuyId 공동구매 ID
     * @return 모든 재고가 소진되었으면 true, 아니면 false
     */
    @Query("SELECT CASE WHEN COUNT(gbo) = 0 THEN true ELSE false END " +
            "FROM GroupBuyOption gbo " +
            "WHERE gbo.groupBuy.id = :groupBuyId " +
            "AND gbo.stock > 0")
    boolean isAllStockDepleted(@Param("groupBuyId") Long groupBuyId);
}