package com.ururulab.ururu.payment.dto.response;

import com.ururulab.ururu.payment.domain.entity.enumerated.PointSource;
import com.ururulab.ururu.payment.domain.entity.enumerated.PointType;

import java.time.Instant;

/**
 * 포인트 거래내역 개별 응답 DTO
 */
public record PointTransactionResponse(
        Long id,
        PointType type,
        PointSource source,
        Integer amount,
        String reason,
        Instant createdAt
) {
}