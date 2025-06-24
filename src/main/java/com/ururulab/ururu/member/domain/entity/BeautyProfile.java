package com.ururulab.ururu.member.domain.entity;

import com.ururulab.ururu.global.common.entity.BaseEntity;
import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.dto.validation.BeautyProfileValidationConstants;
import com.ururulab.ururu.member.domain.dto.validation.BeautyProfileValidationMessages;
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
    @Column(nullable = false)
    private SkinType skinType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private List<String> concerns;

    @Column(nullable = false)
    private boolean hasAllergy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> allergies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON", nullable = false)
    private List<String> interestCategories;

    @Column(nullable = false)
    private int minPrice;

    @Column(nullable = false)
    private int maxPrice;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String additionalInfo;

    public static BeautyProfile of(
            Member member,
            SkinType skinType,
            List<String> concerns,
            boolean hasAllergy,
            List<String> allergies,
            List<String> interestCategories,
            int minPrice,
            int maxPrice,
            String additionalInfo
    ) {
        validateCreationParameters(skinType, concerns, hasAllergy, allergies,
                interestCategories, minPrice, maxPrice, additionalInfo);

        BeautyProfile beautyProfile = new BeautyProfile();
        beautyProfile.member = member;
        beautyProfile.skinType = skinType;
        beautyProfile.concerns = concerns;
        beautyProfile.hasAllergy = hasAllergy;
        beautyProfile.allergies = hasAllergy && allergies != null ? new ArrayList<>(allergies) : new ArrayList<>();
        beautyProfile.interestCategories = interestCategories;
        beautyProfile.minPrice = minPrice;
        beautyProfile.maxPrice = maxPrice;
        beautyProfile.additionalInfo = additionalInfo;
        return beautyProfile;
    }

    public void updateProfile(
            SkinType skinType,
            List<String> concerns,
            boolean hasAllergy,
            List<String> allergies,
            List<String> interestCategories,
            int minPrice,
            int maxPrice,
            String additionalInfo
    ) {
        validateCreationParameters(skinType, concerns, hasAllergy, allergies,
                interestCategories, minPrice, maxPrice, additionalInfo);

        this.skinType = skinType;
        this.concerns = concerns != null ? new ArrayList<>(concerns) : null;
        this.hasAllergy = hasAllergy;
        this.allergies = hasAllergy && allergies != null ? new ArrayList<>(allergies) : null;
        this.interestCategories = interestCategories != null ? new ArrayList<>(interestCategories) : null;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.additionalInfo = additionalInfo;
    }

    private static void validateCreationParameters(
            SkinType skinType,
            List<String> concerns,
            boolean hasAllergy,
            List<String> allergies,
            List<String> interestCategories,
            int minPrice,
            int maxPrice,
            String additionalInfo
    ) {
        validateSkinType(skinType);
        validateConcerns(concerns);
        validateAllergyConsistency(hasAllergy, allergies);
        validateInterestCategories(interestCategories);
        validatePriceRange(minPrice, maxPrice);
        validateAdditionalInfo(additionalInfo);
    }

    private static void validateSkinType(SkinType skinType) {
        if (skinType == null) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.SKIN_TYPE_REQUIRED);
        }
    }

    private static void validateConcerns(List<String> concerns) {
        if (concerns != null) {
            if (concerns.size() > BeautyProfileValidationConstants.MAX_CONCERNS_COUNT) {
                throw new IllegalArgumentException(BeautyProfileValidationMessages.SKIN_CONCERNS_SIZE);
            }
            for (String concern : concerns) {
                if (concern != null && concern.length() > BeautyProfileValidationConstants.CONCERN_ITEM_MAX_LENGTH) {
                    throw new IllegalArgumentException(BeautyProfileValidationMessages.SKIN_CONCERN_ITEM_SIZE);
                }
            }
        }
    }

    private static void validateAllergyConsistency(boolean hasAllergy, List<String> allergies) {
        if (hasAllergy && (allergies == null || allergies.isEmpty())) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.ALLERGY_INCONSISTENCY);
        }
        if (!hasAllergy && allergies != null && !allergies.isEmpty()) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.NO_ALLERGY_INCONSISTENCY);
        }
        if (allergies != null) {
            for (String allergy : allergies) {
                if (allergy != null && allergy.length() > BeautyProfileValidationConstants.ALLERGY_ITEM_MAX_LENGTH) {
                    throw new IllegalArgumentException(BeautyProfileValidationMessages.ALLERGY_ITEM_SIZE);
                }
            }
        }
    }

    private static void validateInterestCategories(List<String> interestCategories) {
        if (interestCategories == null || interestCategories.isEmpty()) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.INTEREST_CATEGORIES_REQUIRED);
        }
        for (String category : interestCategories) {
            if (category != null && category.length() > BeautyProfileValidationConstants.INTEREST_CATEGORY_ITEM_MAX_LENGTH) {
                throw new IllegalArgumentException(BeautyProfileValidationMessages.INTEREST_CATEGORY_ITEM_SIZE);
            }
        }
    }

    private static void validatePriceRange(int minPrice, int maxPrice) {
        if (minPrice < BeautyProfileValidationConstants.MIN_PRICE_VALUE) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.MIN_PRICE_INVALID);
        }
        if (maxPrice < BeautyProfileValidationConstants.MAX_PRICE_VALUE) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.MAX_PRICE_INVALID);
        }
        if (minPrice > maxPrice) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.PRICE_RANGE_INVALID);
        }
    }

    private static void validateAdditionalInfo(String additionalInfo) {
        if (additionalInfo != null && additionalInfo.length() > BeautyProfileValidationConstants.ADDITIONAL_INFO_MAX_LENGTH) {
            throw new IllegalArgumentException(BeautyProfileValidationMessages.ADDITIONAL_INFO_SIZE);
        }
    }
}
