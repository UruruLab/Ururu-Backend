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
@Table(name = "UserAgreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private AgreementType type;

    private boolean agreed;

    private LocalDateTime agreeAt;

    public static UserAgreement of (
            User user,
            AgreementType type,
            boolean agreed
    ) {
        UserAgreement userAgreement = new UserAgreement();
        userAgreement.user = user;
        userAgreement.type = type;
        userAgreement.agreed = agreed;
        userAgreement.agreeAt = agreed ? LocalDateTime.now() : null;
        return userAgreement;
    }
}
