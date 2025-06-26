package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.common.dto.ApiResponseFormat;
import com.ururulab.ururu.member.domain.dto.request.MemberRequest;
import com.ururulab.ururu.member.domain.dto.response.*;
import com.ururulab.ururu.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "회원 관리", description = "회원 정보 조회, 수정, 삭제 및 프로필 이미지 관리")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 정보 조회", description = "특정 회원의 프로필 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponseFormat<GetMemberResponse>> getMemberProfile(
        @PathVariable final Long memberId
    ) {
        final GetMemberResponse response = memberService.getMemberProfile(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("회원 정보를 조회했습니다.", response)
        );
    }

    @Operation(summary = "회원 정보 수정", description = "특정 회원의 프로필 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @PatchMapping("/{memberId}")
    public ResponseEntity<ApiResponseFormat<UpdateMemberResponse>> updateMember(
            @PathVariable final Long memberId,
            @Valid @RequestBody final MemberRequest request
    ) {
        final UpdateMemberResponse response = memberService.updateMemberProfile(memberId, request);
        return ResponseEntity.ok(
                ApiResponseFormat.success("회원 정보를 수정했습니다.", response)
        );
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 프로필 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseFormat<GetMyProfileResponse>> getMyProfile(
    ) {
        final Long memberId = getCurrentMemberId();
        final GetMyProfileResponse response = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("내 정보를 조회했습니다", response)
        );
    }

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 회원의 프로필 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponseFormat<UpdateMyProfileResponse>> updateMyProfile(
            @Valid @RequestBody final MemberRequest request
    ) {
        final Long memberId = getCurrentMemberId(); // 임시
        final UpdateMyProfileResponse response = memberService.updateMyProfile(memberId, request);
        return ResponseEntity.ok(
                ApiResponseFormat.success("내 정보를 수정했습니다.", response)
        );
    }

    @Operation(summary = "프로필 이미지 업로드", description = "회원의 프로필 이미지를 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "프로필 이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 크기"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })

    @PostMapping("/{memberId}/profile-images")
    public ResponseEntity<ApiResponseFormat<Void>> uploadProfileImage(
            @PathVariable final Long memberId,
            @RequestParam("imageFile") final MultipartFile imageFile
    ) {
        memberService.uploadProfileImage(memberId, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("프로필 이미지를 업로드했습니다."));
    }

    @Operation(summary = "프로필 이미지 삭제", description = "회원의 프로필 이미지를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 이미지 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @DeleteMapping("/{memberId}/profile-images")
    public ResponseEntity<ApiResponseFormat<Void>> deleteProfileImage(
            @PathVariable final Long memberId
    ) {
        memberService.deleteProfileImage(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("프로필 이미지를 삭제했습니다.")
        );
    }

    @Operation(summary = "닉네임 존재 여부 확인", description = "특정 닉네임이 이미 사용 중인지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임이 존재함"),
            @ApiResponse(responseCode = "404", description = "닉네임이 존재하지 않음")
    })
    @RequestMapping(value = "/nicknames/{nickname}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkNicknameExists(
            @PathVariable final String nickname
    ) {
        final boolean exists = memberService.checkNicknameExists(nickname);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "닉네임 사용 가능 여부 조회", description = "특정 닉네임의 사용 가능 여부를 상세히 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 사용 가능 여부 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 닉네임 형식")
    })
    @GetMapping("/nicknames/{nickname}/availability")
    public ResponseEntity<ApiResponseFormat<GetNicknameAvailabilityResponse>> getNicknameAvailability(
            @PathVariable final String nickname
    ) {
        final GetNicknameAvailabilityResponse response = memberService.getNicknameAvailability(nickname);
        return ResponseEntity.ok(
                ApiResponseFormat.success("닉네임 사용 가능 여부를 조회했습니다.", response)
        );
    }

    @Operation(summary = "이메일 존재 여부 확인", description = "특정 이메일이 이미 사용 중인지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일이 존재함"),
            @ApiResponse(responseCode = "404", description = "이메일이 존재하지 않음")
    })
    @RequestMapping(value = "/emails/{email}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkEmailExists(
            @PathVariable final String email
    ) {
        final boolean exists = memberService.checkEmailExists(email);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "이메일 사용 가능 여부 조회", description = "특정 이메일의 사용 가능 여부를 상세히 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 사용 가능 여부 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식")
    })
    @GetMapping("/emails/{email}/availability")
    public ResponseEntity<ApiResponseFormat<GetEmailAvailabilityResponse>> getEmailAvailability(
            @PathVariable final String email
    ) {
        final GetEmailAvailabilityResponse response = memberService.getEmailAvailability(email);
        return ResponseEntity.ok(
                ApiResponseFormat.success("이메일 사용 가능 여부를 조회했습니다.", response)
        );
    }

    @Operation(summary = "회원 삭제", description = "관리자가 특정 회원을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 불가능한 상태 (활성 주문 등)"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자 권한 필요)"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseFormat<Void>> deleteMember(
            @PathVariable final Long memberId) {

        try {
            memberService.deleteMember(memberId);
            return ResponseEntity.ok(
                    ApiResponseFormat.success("회원 탈퇴가 완료되었습니다.")
            );
        } catch (IllegalStateException e) {
            // 탈퇴 불가능한 상태 (활성 주문 등)
            log.warn("Member deletion failed due to business rule: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseFormat.fail(e.getMessage()));
        } catch (Exception e) {
            // 기타 시스템 오류
            log.error("Unexpected error during member deletion for ID: {}", memberId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseFormat.fail("탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 회원이 스스로 탈퇴합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "탈퇴 불가능한 상태 (활성 주문 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseFormat<Void>> deleteMyAccount() {
        final Long memberId = getCurrentMemberId();

        try {
            memberService.deleteMember(memberId);
            return ResponseEntity.ok(
                    ApiResponseFormat.success("탈퇴가 완료되었습니다.")
            );
        } catch (IllegalStateException e) {
            log.warn("Member self-deletion failed for ID {}: {}", memberId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponseFormat.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during self-deletion for member ID: {}", memberId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseFormat.fail("탈퇴 처리 중 오류가 발생했습니다."));
        }
    }

    @Operation(summary = "탈퇴 미리보기", description = "회원 탈퇴 시 손실될 정보를 미리 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 미리보기 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/me/withdrawal/priview")
    public ResponseEntity<ApiResponseFormat<GetWithdrawalPreviewResponse>> getWithdrawalPreview() {
        final Long memberId = getCurrentMemberId();
        final GetWithdrawalPreviewResponse response = memberService.getWithdrawalPreview(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("탈퇴 시 손실 정보를 조회했습니다.", response)
        );
    }




    // JWT에서 memberId 추출하는 헬퍼 메서드
    private Long getCurrentMemberId() {
        // TODO: JWT 토큰에서 memberId 추출
        return 1L;
    }

}
