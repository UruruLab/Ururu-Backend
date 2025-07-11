package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberAgreementRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.domain.repository.ShippingAddressRepository;
import com.ururulab.ururu.member.dto.request.MemberRequest;
import com.ururulab.ururu.member.dto.response.*;
import com.ururulab.ururu.order.domain.entity.Cart;
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

import java.time.Instant;
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

    @Transactional
    public Member findOrCreateMember(final SocialMemberInfo socialMemberInfo) {
        return memberRepository.findBySocialProviderAndSocialId(
                socialMemberInfo.provider(),
                socialMemberInfo.socialId()
        ).orElseGet(() -> createNewMember(socialMemberInfo));
    }

    @Transactional(readOnly = true)
    public MemberGetResponse getMyProfile(final Long memberId) {
        final Member member = findActiveMemberById(memberId);
        return MemberGetResponse.from(member);
    }

    @Transactional
    public MemberUpdateResponse updateMyProfile(final Long memberId, final MemberRequest request) {
        final Member updatedMember = updateProfile(memberId, request);
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
        validateMemberDeletion(memberId);
        cleanupMemberRelatedData(memberId);

        member.delete();
        memberRepository.save(member);
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



    private Member updateProfile(final Long memberId, final MemberRequest request) {
        final Member member = findActiveMemberById(memberId);

        if (request.nickname() != null && !request.nickname().equals(member.getNickname())) {
            if (!memberRepository.isNicknameAvailable(request.nickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        updateMemberFields(member, request);

        final Member updatedMember = memberRepository.save(member);
        log.debug("Member profile updated for ID: {}", memberId);

        return updatedMember;
    }


    private Member createNewMember(final SocialMemberInfo socialMemberInfo) {
        final Instant defaultBirthDate = Instant.parse("1990-01-01T00:00:00Z");

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
        log.debug("New social member created with ID: {} for provider: {}",
                savedMember.getId(), socialMemberInfo.provider());

        return savedMember;
    }


    private void updateMemberFields(final Member member, final MemberRequest request) {
        if (request.nickname() != null) { member.updateNickname(request.nickname());}

        if (request.gender() != null) { member.updateGender(parseGender(request.gender()));}

        if (request.birth() != null) { member.updateBirth(request.birth());}

        if (request.phone() != null) { member.updatePhone(request.phone());}
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

    private void validateMemberDeletion(final Long memberId) {
        // TODO: 실제 Repository 구현 후 주석 해제

         int activeOrders = orderRepository.countActiveOrdersByMemberId(memberId);
         if (activeOrders > 0) {
             throw new BusinessException(ErrorCode.MEMBER_ACTIVE_ORDERS_EXIST, activeOrders);
         }

         boolean hasPendingPayments = paymentRepository.existsPendingPaymentsByMemberId(memberId);
         if (hasPendingPayments) {
             throw new BusinessException(ErrorCode.MEMBER_PENDING_PAYMENTS_EXIST);
         }

        // 3. 환불 진행 중인 건 확인
        // boolean hasProcessingRefunds = refundRepository.existsProcessingRefundsByMemberId(memberId);
        // if (hasProcessingRefunds) {
        //     throw new IllegalStateException("환불 처리 중인 건이 있어 탈퇴할 수 없습니다.");
        // }

        log.debug("Member deletion validation passed for ID: {}", memberId);
    }

    private void cleanupMemberRelatedData(final Long memberId) {

        try {
            cleanupCart(memberId);
            shippingAddressRepository.deleteByMemberId(memberId);
            beautyProfileRepository.deleteByMemberId(memberId);
            memberAgreementRepository.deleteByMemberId(memberId);
            handleReviews(memberId);

            log.info("Member related data cleanup completed for ID: {}", memberId);

        } catch (Exception e) {
            log.error("Error during member data cleanup for ID: {}", memberId, e);
            throw new BusinessException(ErrorCode.MEMBER_DELETION_FAILED);
        }
    }

    private void cleanupCart(final Long memberId) {
        Optional<Cart> cartOpt = cartRepository.findByMemberId(memberId);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cart.clearItems();
            cartRepository.delete(cart);
        }
        log.debug("Cart cleanup completed for member ID: {}", memberId);
    }

    private void handleReviews(final Long memberId) {
        // TODO: 구현
        // reviewRepository.deleteByMemberId(memberId);

        log.debug("Reviews handling completed for member ID: {}", memberId);
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