package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.repository.*;
import com.ururulab.ururu.member.dto.request.MemberUpdateRequest;
import com.ururulab.ururu.member.dto.response.*;
import com.ururulab.ururu.order.domain.repository.CartItemRepository;
import com.ururulab.ururu.order.domain.repository.CartRepository;
import com.ururulab.ururu.order.domain.repository.OrderRepository;
import com.ururulab.ururu.payment.domain.repository.PaymentRepository;
import com.ururulab.ururu.payment.domain.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final BeautyProfileRepository beautyProfileRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;

    @Transactional
    public Member findOrCreateMember(final SocialMemberInfo socialMemberInfo) {
        return memberRepository.findBySocialProviderAndSocialId(
                        socialMemberInfo.provider(),
                        socialMemberInfo.socialId()
                )
                .map(member -> {
                    // 탈퇴한 회원인 경우 예외 처리
                    if (member.isDeleted()) {
                        log.warn("Deleted member attempted to login: {} (provider: {})",
                                socialMemberInfo.email(), socialMemberInfo.provider());
                        throw new BusinessException(ErrorCode.MEMBER_LOGIN_DENIED);
                    }
                    return member;
                })
                .orElseGet(() -> createNewMember(socialMemberInfo));
    }

    @Transactional(readOnly = true)
    public MemberGetResponse getMyProfile(final Long memberId) {
        final Member member = findActiveMemberById(memberId);
        return MemberGetResponse.from(member);
    }

    @Transactional
    public MemberUpdateResponse updateMyProfile(final Long memberId, final MemberUpdateRequest request) {
        final Member member = findActiveMemberById(memberId);

        // 업데이트할 필드가 없으면 현재 정보 반환
        if (!request.hasUpdates()) {
            return MemberUpdateResponse.from(member);
        }

        // 닉네임 중복 검사 (변경하려는 경우에만)
        if (request.hasNicknameUpdate() && !request.nickname().equals(member.getNickname())) {
            if (!memberRepository.isNicknameAvailable(request.nickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        updateMemberFields(member, request);

        final Member updatedMember = memberRepository.save(member);
        log.debug("Member profile updated for ID: {}", memberId);

        return MemberUpdateResponse.from(updatedMember);
    }

    @Transactional
    public void uploadProfileImage(final Long memberId, final MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PROFILE_IMAGE_REQUIRED);
        }

        final Member member = findActiveMemberById(memberId);

        // TODO: 이미지 업로드 서비스 구현 후 실제 업로드 처리
        final String imageUrl = "https://example.com/profile/" + memberId + ".jpg"; // 임시

        member.updateProfileImage(imageUrl);
        memberRepository.save(member);

        log.debug("Profile image uploaded for member ID: {}", memberId);
    }

    @Transactional
    public void deleteProfileImage(final Long memberId) {
        final Member member = findActiveMemberById(memberId);

        member.updateProfileImage(null);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public boolean checkNicknameExists(final String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public NicknameAvailabilityResponse getNicknameAvailability(final String nickname) {
        final boolean isAvailable = memberRepository.isNicknameAvailable(nickname);
        return NicknameAvailabilityResponse.from(isAvailable);
    }

    @Transactional(readOnly = true)
    public boolean checkEmailExists(final String email) {
        return memberRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public EmailAvailabilityResponse getEmailAvailability(final String email) {
        final boolean isAvailable = memberRepository.isEmailAvailable(email);
        return EmailAvailabilityResponse.from(isAvailable);
    }

    @Transactional
    public void deleteMember(final Long memberId) {
        final Member member = findActiveMemberById(memberId);

        // 최소한의 검증만
        try {
            int activeOrders = orderRepository.countActiveOrdersByMemberId(memberId);
            if (activeOrders > 0) {
                throw new BusinessException(ErrorCode.MEMBER_ACTIVE_ORDERS_EXIST);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("검증 실패했지만 탈퇴 진행: {}", e.getMessage());
        }

        // 회원만 소프트 삭제 (관련 데이터는 나중에 배치로 정리)
        member.delete();
        memberRepository.save(member);

        log.info("회원 탈퇴 완료 - 회원ID: {} (관련 데이터는 배치에서 정리 예정)", memberId);
    }


    @Transactional(readOnly = true)
    public WithdrawalPreviewResponse getWithdrawalPreview(final Long memberId) {
        final Member member = findActiveMemberById(memberId);

        final WithdrawalPreviewResponse.MemberInfo memberInfo =
                WithdrawalPreviewResponse.MemberInfo.of(
                        member.getNickname(),
                        member.getEmail(),
                        member.getCreatedAt(),
                        member.getProfileImage()
                );

        final WithdrawalPreviewResponse.LossInfo lossInfo = calculateLossInfo(memberId, member);
        return WithdrawalPreviewResponse.of(memberInfo, lossInfo);
    }

    @Transactional(readOnly = true)
    public MemberMyPageResponse getMyPage(final Long memberId) {
        final Member member = findActiveMemberById(memberId);
        final Optional<BeautyProfile> beautyProfileOpt = beautyProfileRepository.findByMemberId(memberId);

        if (beautyProfileOpt.isPresent()) {
            return MemberMyPageResponse.of(member, beautyProfileOpt.get());
        } else {
            return MemberMyPageResponse.from(member);
        }
    }


    private Member createNewMember(final SocialMemberInfo socialMemberInfo) {
        final LocalDate defaultBirthDate = LocalDate.parse("1990-01-01");

        final Member member = Member.of(
                socialMemberInfo.nickname(),
                socialMemberInfo.email(),
                socialMemberInfo.provider(),
                socialMemberInfo.socialId(),
                Gender.FEMALE,
                defaultBirthDate,
                null,
                socialMemberInfo.profileImage(),
                Role.NORMAL
        );

        final Member savedMember = memberRepository.save(member);
        createDefaultBeautyProfile(savedMember);
        log.debug("New social member created with ID: {} for provider: {}",
                savedMember.getId(), socialMemberInfo.provider());

        return savedMember;
    }

    private void createDefaultBeautyProfile(final Member member) {
        try{
            final BeautyProfile defaultProfile = BeautyProfile.of(
                    member,
                    SkinType.NEUTRAL,
                    SkinTone.NEUTRAL,
                    List.of(),
                    false,
                    List.of(),
                    List.of("스킨케어"),
                    10,
                    100000,
                    ""
            );
            beautyProfileRepository.save(defaultProfile);
            log.debug("Default beauty profile created for member ID: {}", member.getId());

        } catch (Exception e) {
            log.warn("Failed to create default beauty profile for member ID: {} - {}",
                    member.getId(), e.getMessage());
        }
    }


    private void updateMemberFields(final Member member, final MemberUpdateRequest request) {
        // 닉네임 업데이트
        if (request.hasNicknameUpdate()) {
            member.updateNickname(request.nickname());
        }

        // 성별 업데이트
        if (request.hasGenderUpdate()) {
            member.updateGender(parseGender(request.gender()));
        }

        // 생년월일 업데이트
        if (request.hasBirthUpdate()) {
            member.updateBirth(request.birth());
        }

        // 전화번호 업데이트
        if (request.hasPhoneUpdate()) {
            member.updatePhone(request.phone());
        }
    }

    private Member findActiveMemberById(final Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> !member.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));
    }

    private Gender parseGender(final String genderString) {
        if (genderString == null) {
            return null;
        }
        try {
            return Gender.from(genderString);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_GENDER_VALUE);
        }
    }

    private WithdrawalPreviewResponse.LossInfo calculateLossInfo(final Long memberId, final Member member) {
        // TODO: 실제 Repository들이 구현되면 아래 주석을 해제하고 실제 데이터 조회


        // int reviewCount = reviewRepository.countByMemberIdAndIsDeleteFalse(memberId);
        int reviewCount = 0; // 임시

        int activeOrders = orderRepository.countActiveOrdersByMemberId(memberId);
        boolean beautyProfileExists = beautyProfileRepository.existsByMemberId(memberId);
        int shippingAddressesCount = shippingAddressRepository.countByMemberId(memberId);
        int memberAgreementsCount = memberAgreementRepository.countByMemberId(memberId);
        int cartItemsCount = cartItemRepository.countByCartMemberId(memberId);
        int pointTransactionsCount = pointTransactionRepository.countByMemberId(memberId);

        return WithdrawalPreviewResponse.LossInfo.of(
                member.getPoint(),
                activeOrders,
                reviewCount,
                beautyProfileExists,
                shippingAddressesCount,
                memberAgreementsCount,
                cartItemsCount,
                pointTransactionsCount
        );
    }
}