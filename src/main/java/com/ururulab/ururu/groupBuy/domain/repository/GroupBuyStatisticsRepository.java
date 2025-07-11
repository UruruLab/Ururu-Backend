package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupBuyStatisticsRepository extends JpaRepository<GroupBuyStatistics, Long> {

    Optional<GroupBuyStatistics> findByGroupBuyId(Long groupBuyId);

    /**
     * 특정 판매자의 모든 그룹 구매 통계 조회
     */
    List<GroupBuyStatistics> findByGroupBuy_SellerId(Long sellerId);

}
