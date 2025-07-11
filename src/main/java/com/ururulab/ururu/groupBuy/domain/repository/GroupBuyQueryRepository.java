package com.ururulab.ururu.groupBuy.domain.repository;

import com.querydsl.core.Tuple;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuySortOption;
import com.ururulab.ururu.groupBuy.dto.common.CursorInfoDto;

import java.util.List;

public interface GroupBuyQueryRepository {
    /**
     * 카테고리 + 필터링 + 무한스크롤 + 키워드 검색
     * @param categoryId
     * @param sortOption
     * @param limit
     * @param cursorInfo
     * @return
     */
    List<Tuple> findGroupBuysSortedWithCursor(
            Long categoryId,
            GroupBuySortOption sortOption,
            int limit,
            CursorInfoDto cursorInfo,
            String keyword // 키워드 추가
            );
}
