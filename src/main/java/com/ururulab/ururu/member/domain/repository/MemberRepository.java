package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN false ELSE true END FROM Member m WHERE m.email = :email AND m.isDeleted = false")
    boolean isEmailAvailable(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN false ELSE true END FROM Member m WHERE m.nickname = :nickname AND m.isDeleted = false")
    boolean isNicknameAvailable(@Param("nickname") String nickname);
}
