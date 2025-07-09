package com.ururulab.ururu.groupBuy.domain.repository;

import com.querydsl.core.Tuple;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuySortOption;

import java.util.List;

public interface GroupBuyQueryRepository {
    List<Tuple> findGroupBuysSorted(Long categoryId, GroupBuySortOption sortOption, int limit);
}
