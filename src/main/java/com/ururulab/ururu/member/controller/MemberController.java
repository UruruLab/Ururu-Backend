package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.common.dto.ApiResponse;
import com.ururulab.ururu.member.domain.dto.response.GetMemberResponse;
import com.ururulab.ururu.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<GetMemberResponse>> getMemberProfile(
        @PathVariable final Long memberId
    ) {
        final GetMemberResponse response = memberService.getMemberProfile(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("회원 정보를 조회했습니다.", response)
        );
    }
}
