package com.ururulab.ururu.order.domain.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.order.domain.entity.Order;
import com.ururulab.ururu.order.domain.entity.enumerated.OrderStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.ururulab.ururu.order.domain.entity.QOrder;
import com.ururulab.ururu.order.domain.entity.QOrderItem;
import com.ururulab.ururu.groupBuy.domain.entity.QGroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.QGroupBuy;
import com.ururulab.ururu.product.domain.entity.QProduct;
import com.ururulab.ururu.payment.domain.entity.QRefundItem;
import com.ururulab.ururu.payment.domain.entity.QRefund;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 회원의 주문 목록을 상태별로 조회합니다.
     * 환불되지 않은 OrderItem이 있는 주문만 반환합니다.
     *
     * @param memberId 회원 ID
     * @param statusFilter 상태 필터 ("inprogress", "confirmed", "refundpending", "all")
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    @Override
    public Page<Order> findMyOrdersWithDetails(Long memberId, String statusFilter, Pageable pageable) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QGroupBuyOption groupBuyOption = QGroupBuyOption.groupBuyOption;
        QGroupBuy groupBuy = QGroupBuy.groupBuy;
        QProduct product = QProduct.product;

        BooleanBuilder builder = buildWhereConditions(memberId, statusFilter);

        // 데이터 조회
        List<Order> orders = queryFactory
                .selectFrom(order)
                .distinct()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.groupBuyOption, groupBuyOption).fetchJoin()
                .leftJoin(groupBuyOption.groupBuy, groupBuy).fetchJoin()
                .leftJoin(groupBuy.product, product).fetchJoin()
                .leftJoin(groupBuyOption.productOption).fetchJoin()  // 직접 조인
                .where(builder)
                .orderBy(order.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 카운트 조회
        Long total = countMyOrders(memberId, statusFilter);

        return new PageImpl<>(orders, pageable, total);
    }

    /**
     * 회원의 주문 개수를 상태별로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param statusFilter 상태 필터
     * @return 주문 개수
     */
    @Override
    public Long countMyOrders(Long memberId, String statusFilter) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QGroupBuyOption groupBuyOption = QGroupBuyOption.groupBuyOption;
        QGroupBuy groupBuy = QGroupBuy.groupBuy;

        BooleanBuilder builder = buildWhereConditions(memberId, statusFilter);

        return queryFactory
                .select(order.countDistinct())
                .from(order)
                .join(order.orderItems, orderItem)
                .join(orderItem.groupBuyOption, groupBuyOption)
                .join(groupBuyOption.groupBuy, groupBuy)
                .where(builder)
                .fetchOne();
    }

    /**
     * 상태별 WHERE 조건을 구성합니다.
     */
    private BooleanBuilder buildWhereConditions(Long memberId, String statusFilter) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QGroupBuy groupBuy = QGroupBuy.groupBuy;

        BooleanBuilder builder = new BooleanBuilder();

        // 공통 조건
        builder.and(order.member.id.eq(memberId))
                .and(order.status.in(OrderStatus.ORDERED, OrderStatus.PARTIAL_REFUNDED))
                .and(hasNonRefundedItems(order));

        // 상태별 조건
        switch (statusFilter.toLowerCase()) {
            case "inprogress" ->
                builder.and(groupBuy.status.eq(GroupBuyStatus.OPEN))
                        .and(hasNoApprovedOrCompletedRefund(orderItem));

            case "confirmed" ->
                builder.and(groupBuy.status.eq(GroupBuyStatus.CLOSED))
                        .and(hasNoApprovedOrCompletedRefund(orderItem));
            case "refundpending" ->
                builder.and(hasInitiatedRefund(orderItem));
            // "all"은 추가 조건 없음 (환불되지 않은 아이템이 있는 조건만)
        }

        return builder;
    }

    /**
     * 환불되지 않은 OrderItem이 하나라도 있는지 확인합니다.
     */
    private BooleanExpression hasNonRefundedItems(QOrder order) {
        QOrderItem oi2 = new QOrderItem("subOrderItem");
        QRefundItem refundItem = QRefundItem.refundItem;
        QRefund refund = QRefund.refund;

        return JPAExpressions
                .selectOne()
                .from(oi2)
                .where(oi2.order.eq(order)
                        .and(JPAExpressions
                                .selectOne()
                                .from(refundItem)
                                .join(refundItem.refund, refund)
                                .where(refundItem.orderItem.eq(oi2)
                                        .and(refund.status.in(
                                                RefundStatus.APPROVED,
                                                RefundStatus.COMPLETED,
                                                RefundStatus.FAILED,
                                                RefundStatus.REJECTED)))
                                .notExists()))
                .exists();
    }

    /**
     * APPROVED 또는 COMPLETED 상태의 환불이 없는지 확인합니다.
     * (기존 로직과 동일)
     */
    private BooleanExpression hasNoApprovedOrCompletedRefund(QOrderItem orderItem) {
        QRefundItem refundItem = QRefundItem.refundItem;
        QRefund refund = QRefund.refund;

        return JPAExpressions
                .selectOne()
                .from(refundItem)
                .join(refundItem.refund, refund)
                .where(refundItem.orderItem.eq(orderItem)
                        .and(refund.status.in(RefundStatus.APPROVED, RefundStatus.COMPLETED)))
                .notExists();
    }

    /**
     * INITIATED 상태의 환불이 있는지 확인합니다.
     */
    private BooleanExpression hasInitiatedRefund(QOrderItem orderItem) {
        QRefundItem refundItem = QRefundItem.refundItem;
        QRefund refund = QRefund.refund;

        return JPAExpressions
                .selectOne()
                .from(refundItem)
                .join(refundItem.refund, refund)
                .where(refundItem.orderItem.eq(orderItem)
                        .and(refund.status.eq(RefundStatus.INITIATED)))
                .exists();
    }
}