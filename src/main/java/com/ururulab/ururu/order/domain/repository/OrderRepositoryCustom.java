package com.ururulab.ururu.order.domain.repository;

import com.ururulab.ururu.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

    /**
     * 회원의 주문 목록을 상태별로 조회합니다.
     * 환불되지 않은 OrderItem이 있는 주문만 반환합니다.
     *
     * @param memberId 회원 ID
     * @param statusFilter 상태 필터 ("inprogress", "confirmed", "refundpending", "all")
     * @param pageable 페이징 정보
     * @return 페이징된 주문 목록
     */
    Page<Order> findMyOrdersWithDetails(Long memberId, String statusFilter, Pageable pageable);

    /**
     * 회원의 주문 개수를 상태별로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param statusFilter 상태 필터
     * @return 주문 개수
     */
    Long countMyOrders(Long memberId, String statusFilter);
}