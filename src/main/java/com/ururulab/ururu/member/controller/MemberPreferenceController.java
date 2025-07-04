package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.member.dto.request.MemberPreferenceRequest;
import com.ururulab.ururu.member.dto.response.MemberPreferenceResponse;
import com.ururulab.ururu.member.service.MemberPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members/preferences")
@RequiredArgsConstructor
@Tag(name = "회원 선호도 관리", description = "회원 선호도 관리 API")
public class MemberPreferenceController {

    private final MemberPreferenceService memberPreferenceService;

    @Operation(summary = "회원 선호도 생성", description = "회원의 판매자별 선호도를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "선호도 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 판매자 선호도")
    })
    @PostMapping
    public ResponseEntity<ApiResponseFormat<MemberPreferenceResponse>> createMemberPreference(
            @AuthenticationPrincipal final Long memberId,
            @Valid @RequestBody final MemberPreferenceRequest request
    ) {
        final MemberPreferenceResponse response = memberPreferenceService.createPreference(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("회원 선호도가 생성되었습니다.", response));
    }

    @Operation(summary = "회원 선호도 목록 조회", description = "특정 회원의 모든 판매자별 선호도를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 선호도 조회 성공"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping()
    public ResponseEntity<ApiResponseFormat<List<MemberPreferenceResponse>>> getMemberPreference(
            @AuthenticationPrincipal final Long memberId
    ){
        final List<MemberPreferenceResponse> responses = memberPreferenceService.getMemberPreferences(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("회원 선호도 목록을 조회했습니다.", responses)
        );
    }
}
