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
import com.ururulab.ururu.groupBuy.dto.common.CursorInfoDto;
import com.ururulab.ururu.product.domain.entity.QProduct;
import com.ururulab.ururu.product.domain.entity.QProductCategory;
import com.ururulab.ururu.product.domain.entity.QProductOption;
import com.ururulab.ururu.seller.domain.entity.QSeller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
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

                        // 최저 시작가 조회
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

    /**
     * 무한스크롤
     * @param categoryId
     * @param sortOption
     * @param limit
     * @param cursorInfo
     * @return
     */
    @Override
    public List<Tuple> findGroupBuysSortedWithCursor(Long categoryId, GroupBuySortOption sortOption, int limit, CursorInfoDto cursorInfo, String keyword) {
        QGroupBuy gb = QGroupBuy.groupBuy;
        QProduct p = QProduct.product;
        QProductCategory pc = QProductCategory.productCategory;
        QGroupBuyOption gbo = QGroupBuyOption.groupBuyOption;
        QProductOption po = QProductOption.productOption;
        QSeller s = QSeller.seller;

        BooleanBuilder where = new BooleanBuilder()
                .and(gb.status.eq(GroupBuyStatus.OPEN))
                .and(gb.endsAt.after(Instant.now()));

        // 키워드 조건
        if (keyword != null && !keyword.isBlank()) {
            BooleanBuilder keywordCondition = new BooleanBuilder()
                    .or(gb.title.containsIgnoreCase(keyword))
                    .or(po.fullIngredients.containsIgnoreCase(keyword))
                    .or(s.name.containsIgnoreCase(keyword));
            where.and(keywordCondition);
        }else {
            log.info("keyword 조건 안 걸림");
        }

        if (categoryId != null) {
            where.and(pc.category.id.eq(categoryId));
        }

        // 커서 조건 추가
        if (cursorInfo != null) {
            where.and(getCursorCondition(sortOption, gb, cursorInfo));
        }

        log.info("➡ [Repo] where = {}", where);

        return queryFactory
                .select(
                        gb.id,
                        gb.title,
                        gb.thumbnailUrl,
                        gb.displayFinalPrice,
                        // 최저 시작가 조회
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
                .join(gb.seller, s)
                .join(gb.product, QProduct.product)
                .join(QProduct.product.productCategories, pc)
                .join(gb.options, gbo)
                .join(gbo.productOption, po)
                .where(where)
                .orderBy(getOrderSpecifier(sortOption, gb))
                .limit(limit)
                .distinct()
                .fetch();
    }

    /**
     * 정렬 옵션에 따른 커서 조건 생성
     */
    private BooleanBuilder getCursorCondition(GroupBuySortOption sort, QGroupBuy gb, CursorInfoDto cursorInfo) {
        BooleanBuilder condition = new BooleanBuilder();

        switch (sort) {
            case LATEST:
                // 최신순: createdAt이 더 이전이거나, 같으면 id가 더 작은 것
                condition.or(gb.createdAt.lt(cursorInfo.createdAt()))
                        .or(gb.createdAt.eq(cursorInfo.createdAt()).and(gb.id.lt(cursorInfo.id())));
                break;

            case DEADLINE:
                // 마감일순: endsAt이 더 늦거나, 같으면 id가 더 작은 것
                condition.or(gb.endsAt.gt(cursorInfo.endsAt()))
                        .or(gb.endsAt.eq(cursorInfo.endsAt()).and(gb.id.lt(cursorInfo.id())));
                break;

            case PRICE_LOW:
                // 가격 낮은순: price가 더 높거나, 같으면 id가 더 작은 것
                condition.or(gb.displayFinalPrice.gt(cursorInfo.price()))
                        .or(gb.displayFinalPrice.eq(cursorInfo.price()).and(gb.id.lt(cursorInfo.id())));
                break;

            case PRICE_HIGH:
                // 가격 높은순: price가 더 낮거나, 같으면 id가 더 작은 것
                condition.or(gb.displayFinalPrice.lt(cursorInfo.price()))
                        .or(gb.displayFinalPrice.eq(cursorInfo.price()).and(gb.id.lt(cursorInfo.id())));
                break;

            case DISCOUNT:
                // 할인율순: DB에서 직접 정렬이 어려우므로 createdAt 기준으로 처리
                condition.or(gb.createdAt.lt(cursorInfo.createdAt()))
                        .or(gb.createdAt.eq(cursorInfo.createdAt()).and(gb.id.lt(cursorInfo.id())));
                break;

            default:
                // 주문량순: DB에서 직접 정렬이 어려우므로 createdAt 기준으로 처리
                condition.or(gb.createdAt.lt(cursorInfo.createdAt()))
                        .or(gb.createdAt.eq(cursorInfo.createdAt()).and(gb.id.lt(cursorInfo.id())));
                break;
        }

        return condition;
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
