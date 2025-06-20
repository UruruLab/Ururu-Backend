package com.ururulab.ururu.user.domain.repository;

import com.ururulab.ururu.user.domain.entity.Member;
import com.ururulab.ururu.user.domain.entity.enumerated.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);
    Optional<Member> findByNickname(String nickname);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN false ELSE true END FROM Member u WHERE u.email = :email AND u.isDeleted = false")
    boolean isEmailAvailable(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN false ELSE true END FROM Member u WHERE u.nickname = :nickname AND u.isDeleted = false")
    boolean isNicknameAvailable(@Param("nickname") String nickname);

}
