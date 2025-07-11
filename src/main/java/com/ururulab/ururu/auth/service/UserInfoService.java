package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 정보 조회 서비스.
 * 토큰 갱신 시 필요한 사용자 정보를 조회하는 역할을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class UserInfoService {

    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;

    /**
     * 사용자 정보를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입 (MEMBER/SELLER)
     * @return 사용자 정보 (이메일, 역할)
     */
    public UserInfo getUserInfo(final Long userId, final String userType) {
                                    if (UserType.SELLER.getValue().equals(userType)) {
            return getSellerInfo(userId);
        } else {
            return getMemberInfo(userId);
        }
    }

    /**
     * 판매자 정보를 조회합니다.
     *
     * @param sellerId 판매자 ID
     * @return 판매자 정보
     * @throws BusinessException 판매자를 찾을 수 없는 경우
     */
    private UserInfo getSellerInfo(final Long sellerId) {
        final Seller seller = sellerRepository.findActiveSeller(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_NOT_FOUND));
        
        return UserInfo.of(
                seller.getEmail(),
                                                    UserRole.SELLER.getValue()
        );
    }

    /**
     * 회원 정보를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 회원 정보
     * @throws BusinessException 회원을 찾을 수 없는 경우
     */
    private UserInfo getMemberInfo(final Long memberId) {
        final Member member = memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        
        return UserInfo.of(
                member.getEmail(),
                member.getRole().name()
        );
    }

    /**
     * 사용자 정보를 담는 불변 객체.
     */
    public record UserInfo(String email, String role) {
        public static UserInfo of(final String email, final String role) {
            return new UserInfo(email, role);
        }
    }
} 