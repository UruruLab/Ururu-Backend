package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);
    Optional<Member> findByNickname(String nickname);

    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN false ELSE true END FROM Member m WHERE m.email = :email AND m.isDeleted = false")
    boolean isEmailAvailable(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN false ELSE true END FROM Member m WHERE m.nickname = :nickname AND m.isDeleted = false")
    boolean isNicknameAvailable(@Param("nickname") String nickname);

    @Query("SELECT m FROM Member m WHERE m.id = :memberId AND m.isDeleted = false")
    Optional<Member> findForWithdrawalPreview(@Param("memberId") Long memberId);

    @Query("SELECT m FROM Member m WHERE m.isDeleted = false ORDER BY m.updatedAt DESC")
    List<Member> findRecentActiveMembers(Pageable pageable);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.beautyProfile LEFT JOIN FETCH m.shippingAddresses WHERE m.id = :memberId AND m.isDeleted = false")
    Optional<Member> findForDashboard(@Param("memberId") Long memberId);

    /**
     * 회원 포인트 차감
     * 결제 시 포인트 사용 등에 사용
     */
    @Modifying
    @Query("UPDATE Member m SET m.point = m.point - :amount WHERE m.id = :memberId AND m.point >= :amount")
    int decreasePoints(@Param("memberId") Long memberId, @Param("amount") Integer amount);

    /**
     * 회원 포인트 증가
     * 포인트 적립, 복구 등에 사용
     */
    @Modifying
    @Query("UPDATE Member m SET m.point = m.point + :amount WHERE m.id = :memberId")
    void increasePoints(@Param("memberId") Long memberId, @Param("amount") Integer amount);

}
