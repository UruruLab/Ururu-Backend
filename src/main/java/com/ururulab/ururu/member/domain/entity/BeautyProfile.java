package com.ururulab.ururu.member.domain.entity;

import com.ururulab.ururu.global.domain.entity.BaseEntity;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.policy.BeautyProfilePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "beauty_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BeautyProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkinType skinType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkinTone skinTone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private List<String> concerns;

    @Column(nullable = false)
    private Boolean hasAllergy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> allergies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private List<String> interestCategories;

    @Column(nullable = false)
    private Integer minPrice;

    @Column(nullable = false)
    private Integer maxPrice;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String additionalInfo;

    public static BeautyProfile of(
            Member member,
            SkinType skinType,
            SkinTone skinTone,
            List<String> concerns,
            Boolean hasAllergy,
            List<String> allergies,
            List<String> interestCategories,
            Integer minPrice,
            Integer maxPrice,
            String additionalInfo
    ) {
        validateCreationParameters(hasAllergy, allergies, minPrice, maxPrice);

        BeautyProfile beautyProfile = new BeautyProfile();
        beautyProfile.member = member;
        beautyProfile.skinType = skinType;
        beautyProfile.skinTone = skinTone;
        beautyProfile.concerns = concerns != null ? new ArrayList<>(concerns) : new ArrayList<>();
        beautyProfile.hasAllergy = hasAllergy;
        beautyProfile.allergies = hasAllergy && allergies != null ? new ArrayList<>(allergies) : new ArrayList<>();
        beautyProfile.interestCategories = interestCategories != null ? new ArrayList<>(interestCategories) : new ArrayList<>();
        beautyProfile.minPrice = minPrice;
        beautyProfile.maxPrice = maxPrice;
        beautyProfile.additionalInfo = additionalInfo != null ? additionalInfo : "";
        return beautyProfile;
    }

    public void updateProfile(
            SkinType skinType,
            SkinTone skinTone,
            List<String> concerns,
            Boolean hasAllergy,
            List<String> allergies,
            List<String> interestCategories,
            Integer minPrice,
            Integer maxPrice,
            String additionalInfo
    ) {
        validateCreationParameters(hasAllergy, allergies, minPrice, maxPrice);

        this.skinType = skinType;
        this.skinTone = skinTone;
        this.concerns = concerns != null ? new ArrayList<>(concerns) : new ArrayList<>();
        this.hasAllergy = hasAllergy;
        this.allergies = hasAllergy && allergies != null ? new ArrayList<>(allergies) : new ArrayList<>();
        this.interestCategories = interestCategories != null ? new ArrayList<>(interestCategories) : new ArrayList<>();
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.additionalInfo = additionalInfo != null ? additionalInfo : "";
    }

    private static void validateCreationParameters(
            Boolean hasAllergy,
            List<String> allergies,
            Integer minPrice,
            Integer maxPrice
    ) {

        validateAllergyConsistency(hasAllergy, allergies);
        validatePriceRange(minPrice, maxPrice);
    }


    private static void validateAllergyConsistency(Boolean hasAllergy, List<String> allergies) {
        if (hasAllergy && (allergies == null || allergies.isEmpty())) {
            throw new IllegalArgumentException(BeautyProfilePolicy.ALLERGY_INCONSISTENCY);
        }
        if (!hasAllergy && allergies != null && !allergies.isEmpty()) {
            throw new IllegalArgumentException(BeautyProfilePolicy.NO_ALLERGY_INCONSISTENCY);
        }
        if (allergies != null) {
            for (String allergy : allergies) {
                if (allergy != null && allergy.length() > BeautyProfilePolicy.ALLERGY_ITEM_MAX_LENGTH) {
                    throw new IllegalArgumentException(BeautyProfilePolicy.ALLERGY_ITEM_SIZE);
                }
            }
        }
    }

    private static void validatePriceRange(Integer minPrice, Integer maxPrice) {
        if (minPrice > maxPrice) {
            throw new IllegalArgumentException(BeautyProfilePolicy.PRICE_MIN_MAX_COMPARE);
        }
    }
}
