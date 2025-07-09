package com.ururulab.ururu.groupBuy.domain.repository.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ururulab.ururu.groupBuy.domain.entity.QGroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.QGroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuySortOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyQueryRepository;
import com.ururulab.ururu.product.domain.entity.QProduct;
import com.ururulab.ururu.product.domain.entity.QProductCategory;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class GroupBuyQueryRepositoryImpl implements GroupBuyQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Tuple> findGroupBuysSorted(Long categoryId, GroupBuySortOption sortOption, int limit) {
        QGroupBuy gb = QGroupBuy.groupBuy;
        QProduct p = QProduct.product;
        QProductCategory pc = QProductCategory.productCategory;
        QGroupBuyOption gbo = QGroupBuyOption.groupBuyOption;

        BooleanBuilder where = new BooleanBuilder()
                .and(gb.status.eq(GroupBuyStatus.OPEN))
                .and(gb.endsAt.after(Instant.now()));

        if (categoryId != null) {
            where.and(pc.category.id.eq(categoryId));
        }

        return queryFactory
                .select(
                        gb.id,
                        gb.title,
                        gb.thumbnailUrl,
                        gb.displayFinalPrice,

                        // 최저 시작가 조회 (변경 없음)
                        JPAExpressions.select(gbo.priceOverride.min())
                                .from(gbo)
                                .where(gbo.groupBuy.id.eq(gb.id)),

                        gb.discountStages,
                        gb.endsAt,

                        // initialStock 기반 총 판매량 조회
                        JPAExpressions.select(
                                        gbo.initialStock.sum().subtract(gbo.stock.sum()).coalesce(0)
                                )
                                .from(gbo)
                                .where(gbo.groupBuy.id.eq(gb.id)),

                        gb.createdAt
                )
                .from(gb)
                .join(gb.product, p)
                .join(p.productCategories, pc)
                .where(where)
                .orderBy(getOrderSpecifier(sortOption, gb))
                .limit(limit)
                .fetch();
    }

    private OrderSpecifier<?> getOrderSpecifier(GroupBuySortOption sort, QGroupBuy gb) {
        return switch (sort) {
            case LATEST -> gb.createdAt.desc(); // 생성순
            case DEADLINE -> gb.endsAt.asc();
            case PRICE_LOW -> gb.displayFinalPrice.asc();
            case PRICE_HIGH -> gb.displayFinalPrice.desc();
            case DISCOUNT -> gb.createdAt.desc(); // JSON 형식을 직접 추출 불가 - 계산 위임
            default -> gb.createdAt.desc(); // 주문 많은 순 - 계산 위임
        };
    }
}
