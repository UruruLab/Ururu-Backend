package com.ururulab.ururu.user.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.user.domain.entity.enumerated.SkinType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Getter
@Table(name = "BeautyProfile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BeautyProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    private SkinType skinType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> concerns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> allergies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> interestCategories;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    public static BeautyProfile of(
            Member member,
            SkinType skinType,
            List<String> concerns,
            List<String> allergies,
            List<String> interestCategories,
            String additionalInfo
    ) {
        BeautyProfile beautyProfile = new BeautyProfile();
        beautyProfile.member = member;
        beautyProfile.skinType = skinType;
        beautyProfile.concerns = concerns;
        beautyProfile.allergies = allergies;
        beautyProfile.interestCategories = interestCategories;
        beautyProfile.additionalInfo = additionalInfo;
        return beautyProfile;
    }
}
