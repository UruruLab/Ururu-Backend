package com.ururulab.ururu.member.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "Member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String nickname;

    @Column(length = 100, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private SocialProvider socialProvider;

    @Column(length = 100)
    private String socialId;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birth;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    private Role role;

    private int point = 0;

    private boolean isDeleted = false;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private BeautyProfile beautyProfile;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<ShippingAddress> shippingAddresses = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<MemberAgreement> memberAgreements = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<MemberPreference> memberPreferences = new ArrayList<>();


    public static Member of(
            String nickname,
            String email,
            SocialProvider socialProvider,
            String socialId,
            Gender gender,
            LocalDate birth,
            String phone,
            String profileImage,
            Role role
    ) {
        Member member = new Member();
        member.nickname = nickname;
        member.email = email;
        member.socialProvider = socialProvider;
        member.socialId = socialId;
        member.gender = gender;
        member.birth = birth;
        member.phone = phone;
        member.profileImage = profileImage;
        member.role = role != null ? role : Role.NORMAL;
        return member;
    }

    public void updateNickname(final String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        this.nickname = nickname.trim();
    }

    public void updateGender(final Gender gender) {
        this.gender = gender;
    }

    public void updateBirth(final LocalDate birth) {
        this.birth = birth;
    }

    public void updatePhone(final String phone) {
        this.phone = phone;
    }
}
