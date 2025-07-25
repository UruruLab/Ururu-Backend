package com.ururulab.ururu.member.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.member.dto.request.MemberUpdateRequest;
import com.ururulab.ururu.member.dto.response.*;
import com.ururulab.ururu.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "회원 관리", description = "회원 정보 조회, 수정, 삭제 및 프로필 이미지 관리")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 프로필 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponseFormat<MemberGetResponse>> getMyProfile(
            @AuthenticationPrincipal final Long memberId
    ) {
        final MemberGetResponse response = memberService.getMyProfile(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("내 정보를 조회했습니다", response)
        );
    }

    @Operation(summary = "내 정보 수정", description = "현재 로그인한 회원의 프로필 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "409", description = "중복된 닉네임")
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponseFormat<MemberUpdateResponse>> updateMyProfile(
            @AuthenticationPrincipal final Long memberId,
            @Valid @RequestBody final MemberUpdateRequest request
    ) {
        final MemberUpdateResponse response = memberService.updateMyProfile(memberId, request);
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
    @PostMapping("/profile-images")
    public ResponseEntity<ApiResponseFormat<Void>> uploadProfileImage(
            @AuthenticationPrincipal final Long memberId,
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
    @DeleteMapping("/profile-images")
    public ResponseEntity<ApiResponseFormat<Void>> deleteProfileImage(
            @AuthenticationPrincipal final Long memberId
    ) {
        memberService.deleteProfileImage(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("프로필 이미지를 삭제했습니다.")
        );
    }

    @Operation(summary = "닉네임 존재 여부 확인", description = "특정 닉네임이 이미 사용 중인지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임이 존재 여부 확인 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 닉네임 형식")
    })
    @GetMapping(value = "/nicknames/{nickname}/exists")
    public ResponseEntity<ApiResponseFormat<Object>> checkNicknameExists(
            @PathVariable final String nickname
    ) {
        final boolean exists = memberService.checkNicknameExists(nickname);
        final Object response = Map.of(
                "nickname", nickname,
                "exists", exists
        );
        return ResponseEntity.ok(
                ApiResponseFormat.success("닉네임 존재 여부를 확인했습니다.", response)
        );
    }

    @Operation(summary = "닉네임 사용 가능 여부 조회", description = "특정 닉네임의 사용 가능 여부를 상세히 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 사용 가능 여부 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 닉네임 형식")
    })
    @GetMapping("/nicknames/{nickname}/availability")
    public ResponseEntity<ApiResponseFormat<NicknameAvailabilityResponse>> getNicknameAvailability(
            @PathVariable final String nickname
    ) {
        final NicknameAvailabilityResponse response = memberService.getNicknameAvailability(nickname);
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
    public ResponseEntity<ApiResponseFormat<EmailAvailabilityResponse>> getEmailAvailability(
            @PathVariable final String email
    ) {
        final EmailAvailabilityResponse response = memberService.getEmailAvailability(email);
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
            @PathVariable final Long memberId
    ) {
        memberService.deleteMember(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("회원 탈퇴가 완료되었습니다.")
        );

    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 회원이 스스로 탈퇴합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "탈퇴 불가능한 상태 (활성 주문 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponseFormat<Void>> deleteMyAccount(
            @AuthenticationPrincipal final Long memberId
    ) {
        memberService.deleteMember(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("탈퇴가 완료되었습니다.")
        );
    }

    @Operation(summary = "탈퇴 미리보기", description = "회원 탈퇴 시 손실될 정보를 미리 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 미리보기 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/me/withdrawal/preview")
    public ResponseEntity<ApiResponseFormat<WithdrawalPreviewResponse>> getWithdrawalPreview(
            @AuthenticationPrincipal final Long memberId
    ) {
        final WithdrawalPreviewResponse response = memberService.getWithdrawalPreview(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("탈퇴 시 손실 정보를 조회했습니다.", response)
        );
    }

    @Operation(summary = "마이페이지 조회", description = "회원의 마이페이지 정보를 조회합니다. 닉네임, 프로필 이미지, 포인트, 뷰티 프로필 정보를 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    @GetMapping("/me/mypage")
    public ResponseEntity<ApiResponseFormat<MemberMyPageResponse>> getMyPage(
            @AuthenticationPrincipal final Long memberId
    ) {
        final MemberMyPageResponse response = memberService.getMyPage(memberId);
        return ResponseEntity.ok(
                ApiResponseFormat.success("마이페이지 정보를 조회했습니다.", response)
        );
    }
}
