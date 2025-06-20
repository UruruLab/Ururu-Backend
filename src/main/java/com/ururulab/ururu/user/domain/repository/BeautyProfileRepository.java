package com.ururulab.ururu.user.domain.repository;

import com.ururulab.ururu.user.domain.entity.BeautyProfile;
import com.ururulab.ururu.user.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeautyProfileRepository extends JpaRepository<BeautyProfile, Long> {
    Optional<BeautyProfile> findByMember(Member member);
    Optional<BeautyProfile> findByMemberId(Long memberId);

    @Query("SELECT bp FROM BeautyProfile bp JOIN FETCH bp.member WHERE bp.member.id = :memberId")
    Optional<BeautyProfile> findByUserIdWithUser(@Param("memberId") Long memberId);

}
