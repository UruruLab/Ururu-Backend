package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.common.dto.ApiResponse;
import com.ururulab.ururu.member.domain.dto.request.MemberRequest;
import com.ururulab.ururu.member.domain.dto.response.GetMemberResponse;
import com.ururulab.ururu.member.domain.dto.response.UpdateMemberResponse;
import com.ururulab.ururu.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/{memberId}")
    public ResponseEntity<ApiResponse<UpdateMemberResponse>> updateMember(
            @PathVariable final Long memberId,
            @Valid @RequestBody final MemberRequest request
    ) {
        final UpdateMemberResponse response = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(
                ApiResponse.success("회원 정보를 수정했습니다.", response)
        );
    }
}
