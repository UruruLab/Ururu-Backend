package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.common.dto.ApiResponse;
import com.ururulab.ururu.member.domain.dto.request.MemberRequest;
import com.ururulab.ururu.member.domain.dto.response.*;
import com.ururulab.ururu.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
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
    public ResponseEntity<ApiResponse<GetMyProfileResponse>> getMyProfile(
    ) {
        final Long memberId = getCurrentMemberId();
        final GetMyProfileResponse response = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("내 정보를 조회했습니다", response)
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UpdateMyProfileResponse>> updateMyProfile(
            @Valid @RequestBody final MemberRequest request
    ) {
        final Long memberId = getCurrentMemberId(); // 임시
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

    @RequestMapping(value = "/emails/{email}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkEmailExists(
            @PathVariable final String email
    ) {
        final boolean exists = memberService.checkEmailExists(email);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/emails/{email}/availability")
    public ResponseEntity<ApiResponse<GetEmailAvailabilityResponse>> getEmailAvailability(
            @PathVariable final String email
    ) {
        final GetEmailAvailabilityResponse response = memberService.getEmailAvailability(email);
        return ResponseEntity.ok(
                ApiResponse.success("이메일 사용 가능 여부를 조회했습니다.", response)
        );
    }

    @DeleteMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능 (추후 구현)
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable final Long memberId) {

        try {
            memberService.deleteMember(memberId);
            return ResponseEntity.ok(
                    ApiResponse.success("회원 탈퇴가 완료되었습니다.")
            );
        } catch (IllegalStateException e) {
            // 탈퇴 불가능한 상태 (활성 주문 등)
            log.warn("Member deletion failed due to business rule: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            // 기타 시스템 오류
            log.error("Unexpected error during member deletion for ID: {}", memberId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount() {
        final Long memberId = getCurrentMemberId();

        try {
            memberService.deleteMember(memberId);
            return ResponseEntity.ok(
                    ApiResponse.success("탈퇴가 완료되었습니다.")
            );
        } catch (IllegalStateException e) {
            log.warn("Member self-deletion failed for ID {}: {}", memberId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during self-deletion for member ID: {}", memberId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/me/withdrawal/priview")
    public ResponseEntity<ApiResponse<GetWithdrawalPreviewResponse>> getWithdrawalPreview() {
        final Long memberId = getCurrentMemberId();
        final GetWithdrawalPreviewResponse response = memberService.getWithdrawalPreview(memberId);
        return ResponseEntity.ok(
                ApiResponse.success("탈퇴 시 손실 정보를 조회했습니다.", response)
        );
    }




    // JWT에서 memberId 추출하는 헬퍼 메서드
    private Long getCurrentMemberId() {
        // TODO: JWT 토큰에서 memberId 추출
        return 1L;
    }

}
