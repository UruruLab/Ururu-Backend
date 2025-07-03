package com.ururulab.ururu.member.domain.repository;

import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeautyProfileRepository extends JpaRepository<BeautyProfile, Long> {
    Optional<BeautyProfile> findByMemberId(Long memberId);

    @Query("SELECT bp FROM BeautyProfile bp JOIN FETCH bp.member WHERE bp.member.id = :memberId")
    Optional<BeautyProfile> findByMemberIdWithMember(@Param("memberId") Long memberId);
    boolean existsByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
