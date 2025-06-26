package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.common.dto.ApiResponse;
import com.ururulab.ururu.member.domain.dto.request.MemberRequest;
import com.ururulab.ururu.member.domain.dto.response.*;
import com.ururulab.ururu.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        final UpdateMemberResponse response = memberService.updateMemberProfile(memberId, request);
        return ResponseEntity.ok(
                ApiResponse.success("회원 정보를 수정했습니다.", response)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyProfileResponse>> getMyProfile() {
        // TODO: JWT 토큰에서 memberId 추출
        final Long memberId = 1L;
        final GetMyProfileResponse response = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("내 정보를 조회했습니다", response)
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UpdateMyProfileResponse>> updateMyProfile(
            @Valid @RequestBody final MemberRequest request
    ) {
        // TODO: JWT 토큰에서 memberId 추출
        final Long memberId = 1L; // 임시
        final UpdateMyProfileResponse response = memberService.updateMyProfile(memberId, request);
        return ResponseEntity.ok(
                ApiResponse.success("내 정보를 수정했습니다.", response)
        );
    }

    @PostMapping("/{memberId}/profile-images")
    public ResponseEntity<ApiResponse<Void>> uploadProfileImage(
            @PathVariable final Long memberId,
            @RequestParam("imageFile") final MultipartFile imageFile
    ) {
        memberService.uploadProfileImage(memberId, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("프로필 이미지를 업로드했습니다."));
    }

    @DeleteMapping("/{memberId}/profile-images")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage(
            @PathVariable final Long memberId
    ) {
        memberService.deleteProfileImage(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("프로필 이미지를 삭제했습니다.")
        );
    }

    @RequestMapping(value = "/nicknames/{nickname}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkNicknameExists(
            @PathVariable final String nickname
    ) {
        final boolean exists = memberService.checkNicknameExists(nickname);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/nicknames/{nickname}/availability")
    public ResponseEntity<ApiResponse<GetNicknameAvailabilityResponse>> getNicknameAvailability(
            @PathVariable final String nickname
    ) {
        final GetNicknameAvailabilityResponse response = memberService.getNicknameAvailability(nickname);
        return ResponseEntity.ok(
                ApiResponse.success("닉네임 사용 가능 여부를 조회했습니다.", response)
        );
    }
}
