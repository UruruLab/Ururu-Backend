package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.common.dto.ApiResponseFormat;
import com.ururulab.ururu.member.domain.dto.request.BeautyProfileRequest;
import com.ururulab.ururu.member.domain.dto.response.CreateBeautyProfileResponse;
import com.ururulab.ururu.member.domain.dto.response.GetBeautyProfileResponse;
import com.ururulab.ururu.member.domain.dto.response.UpdateBeautyProfileResponse;
import com.ururulab.ururu.member.service.BeautyProfileService;
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
@Tag(name = "뷰티 프로필 관리", description = "회원의 뷰티 프로필 생성, 조회, 수정, 삭제")
public class BeautyProfileController {

    private final BeautyProfileService beautyProfileService;

    @Operation(summary = "뷰티 프로필 생성", description = "특정 회원의 뷰티 프로필을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "뷰티 프로필 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 뷰티 프로필이 존재함")
    })
    @PostMapping("/{memberId}/beauty-profile")
    public ResponseEntity<ApiResponseFormat<CreateBeautyProfileResponse>> createBeautyProfile(
            @PathVariable final Long memberId,
            @Valid @RequestBody final BeautyProfileRequest request
    ){
      final CreateBeautyProfileResponse response = beautyProfileService.createBeautyProfile(memberId, request);
      return ResponseEntity.status(HttpStatus.CREATED)
              .body(ApiResponseFormat.success("뷰티 프로필이 생성되었습니다.", response));
    }

    @Operation(summary = "뷰티 프로필 조회", description = "특정 회원의 뷰티 프로필을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "뷰티 프로필 조회 성공"),
            @ApiResponse(responseCode = "404", description = "회원 또는 뷰티 프로필을 찾을 수 없음")
    })
    @GetMapping("/{memberId}/beauty-profile")
    public ResponseEntity<ApiResponseFormat<GetBeautyProfileResponse>> getBeautyProfile(
            @PathVariable final Long memberId
    ) {
        final GetBeautyProfileResponse response = beautyProfileService.getBeautyProfile(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("뷰티 프로필을 조회했습니다.", response)
        );
    }

    @Operation(summary = "뷰티 프로필 수정", description = "특정 회원의 뷰티 프로필을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "뷰티 프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "회원 또는 뷰티 프로필을 찾을 수 없음")
    })
    @PatchMapping("/{memberId}/beauty-profile")
    public ResponseEntity<ApiResponseFormat<UpdateBeautyProfileResponse>> updateBeautyProfile(
            @PathVariable final Long memberId,
            @Valid @RequestBody final BeautyProfileRequest request
    ) {
        final UpdateBeautyProfileResponse response = beautyProfileService.updateBeautyProfile(memberId, request);
        return ResponseEntity.ok(
                ApiResponseFormat.success("뷰티 프로필이 수정되었습니다.", response)
        );
    }
}
