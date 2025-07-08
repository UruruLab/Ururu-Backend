package com.ururulab.ururu.groupBuy.domain.repository;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupBuyStatisticsRepository extends JpaRepository<GroupBuyStatistics, Long> {

    /**
     * 공구 ID로 통계 조회
     */
    @Query("SELECT gbs FROM GroupBuyStatistics gbs WHERE gbs.groupBuy.id = :groupBuyId")
    Optional<GroupBuyStatistics> findByGroupBuyId(@Param("groupBuyId") Long groupBuyId);
}
