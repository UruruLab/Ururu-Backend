package com.ururulab.ururu.member.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.member.domain.entity.enumerated.AgreementType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Getter
@Table(name = "members_agreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementType type;

    @Column(nullable = false)
    private boolean agreed;

    private ZonedDateTime agreeAt;

    public static MemberAgreement of (
            Member member,
            AgreementType type,
            boolean agreed
    ) {
        MemberAgreement memberAgreement = new MemberAgreement();
        memberAgreement.member = member;
        memberAgreement.type = type;
        memberAgreement.agreed = agreed;
        memberAgreement.agreeAt = agreed ? ZonedDateTime.now() : null;
        return memberAgreement;
    }
}
