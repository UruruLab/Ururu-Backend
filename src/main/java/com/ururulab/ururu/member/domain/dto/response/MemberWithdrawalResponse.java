package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MemberWithdrawalResponse(
        @JsonProperty("member_id") Long memberId,
        String message
) {
    public static MemberWithdrawalResponse success(final Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID는 필수입니다.");
        }

        return new MemberWithdrawalResponse(
                memberId,
                "회원 탈퇴가 정상적으로 처리되었습니다."
        );
    }
}
