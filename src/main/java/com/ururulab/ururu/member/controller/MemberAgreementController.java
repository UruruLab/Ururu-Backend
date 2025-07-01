package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.member.domain.dto.request.MemberAgreementRequest;
import com.ururulab.ururu.member.domain.dto.response.MemberAgreementCreateResponse;
import com.ururulab.ururu.member.service.MemberAgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원 약관 동의 관리", description = "회원 약관 동의 관리")
public class MemberAgreementController {

    private final MemberAgreementService memberAgreementService;

    @Operation(summary = "회원 약관 동의 생성", description = "특정 회원의 약관 동의를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원 약관 동의 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PostMapping("/{memberId}/agreements")
    public ResponseEntity<ApiResponseFormat<MemberAgreementCreateResponse>> createMemberAgreement(
            @PathVariable final Long memberId,
            @Valid @RequestBody final MemberAgreementRequest request
    ){
        final MemberAgreementCreateResponse response = memberAgreementService.createAgreements(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("회원 약관 동의가 생성되었습니다.", response));
    }
}
