package com.ururulab.ururu.auth.service.social;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 회원 관련 트랜잭션 처리를 위한 서비스.
 *
 * <p>Spring AOP 프록시의 자가 호출 문제를 해결하기 위해 별도 컴포넌트로 분리하였습니다.
 * 소셜 로그인 시 회원 조회/생성 로직만 트랜잭션 범위에 포함하여 성능을 최적화합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTransactionService {

    private final MemberRepository memberRepository;

    /**
     * 회원 조회 또는 신규 생성을 트랜잭션으로 처리.
     *
     * @param socialMemberInfo 소셜 로그인에서 가져온 회원 정보
     * @return 조회되거나 새로 생성된 회원
     */
    @Transactional
    public Member findOrCreateMember(final SocialMemberInfo socialMemberInfo) {
        final Optional<Member> existingMember = memberRepository
                .findBySocialProviderAndSocialId(
                        socialMemberInfo.provider(),
                        socialMemberInfo.socialId()
                );

        if (existingMember.isPresent()) {
            final Member member = existingMember.get();
            log.debug("Existing member found: {}", member.getId());
            return member;
        }

        final Member newMember = Member.of(
                socialMemberInfo.nickname(),
                socialMemberInfo.email(),
                socialMemberInfo.provider(),
                socialMemberInfo.socialId(),
                null, // gender - 카카오에서 제공하지 않음
                null, // birth - 카카오에서 제공하지 않음
                null, // phone - 카카오에서 제공하지 않음
                socialMemberInfo.profileImage(),
                Role.NORMAL // 구매자는 NORMAL 권한
        );

        final Member savedMember = memberRepository.save(newMember);
        log.info("New buyer member created via {}: {}",
                socialMemberInfo.provider(), savedMember.getId());

        return savedMember;
    }
}