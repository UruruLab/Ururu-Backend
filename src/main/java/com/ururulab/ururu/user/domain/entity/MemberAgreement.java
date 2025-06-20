package com.ururulab.ururu.user.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.user.domain.entity.enumerated.AgreementType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "MemberAgreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    private AgreementType type;

    private boolean agreed;

    private LocalDateTime agreeAt;

    public static MemberAgreement of (
            Member member,
            AgreementType type,
            boolean agreed
    ) {
        MemberAgreement memberAgreement = new MemberAgreement();
        memberAgreement.member = member;
        memberAgreement.type = type;
        memberAgreement.agreed = agreed;
        memberAgreement.agreeAt = agreed ? LocalDateTime.now() : null;
        return memberAgreement;
    }
}
