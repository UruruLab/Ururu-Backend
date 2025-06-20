package com.ururulab.ururu.user.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.user.domain.entity.enumerated.Gender;
import com.ururulab.ururu.user.domain.entity.enumerated.Role;
import com.ururulab.ururu.user.domain.entity.enumerated.SocialProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "User")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

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

    private LocalDateTime birth;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    private Role role;

    private int point = 0;

    private boolean isDeleted = false;

    public static User of(
            String nickname,
            String email,
            SocialProvider socialProvider,
            String socialId,
            Gender gender,
            LocalDateTime birth,
            String phone,
            String profileImage,
            Role role
    ) {
        User user = new User();
        user.nickname = nickname;
        user.email = email;
        user.socialProvider = socialProvider;
        user.socialId = socialId;
        user.gender = gender;
        user.birth = birth;
        user.phone = phone;
        user.profileImage = profileImage;
        user.role = role != null ? role : Role.NORMAL;
        return user;
    }
}
