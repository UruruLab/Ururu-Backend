package com.ururulab.ururu.payment.dto.response;

/**
 * 현재 포인트 조회 응답 DTO
 * GET /api/member/me/points
 */
public record MemberPointResponse(
        Integer currentPoints
) {
}