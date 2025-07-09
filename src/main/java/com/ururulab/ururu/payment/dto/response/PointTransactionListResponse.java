package com.ururulab.ururu.payment.dto.response;

import java.util.List;

/**
 * 포인트 거래내역 목록 응답 DTO (페이징 포함)
 * GET /api/member/me/point-transactions
 */
public record PointTransactionListResponse(
        List<PointTransactionResponse> transactions,
        Integer page,
        Integer size,
        Long total,
        Integer totalPages
) {
}