package com.ururulab.ururu.member.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.validation.EnumValue;
import com.ururulab.ururu.member.controller.dto.validation.MemberValidationConstants;
import com.ururulab.ururu.member.controller.dto.validation.MemberValidationMessages;
import com.ururulab.ururu.member.controller.dto.validation.MemberValidationPatterns;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.validation.constraints.*;

import java.time.LocalDate;


public record MemberRequest(
        @NotBlank(message = MemberValidationMessages.NICKNAME_REQUIRED)
        @Size(min = MemberValidationConstants.NICKNAME_MIN_LENGTH,
                max = MemberValidationConstants.NICKNAME_MAX_LENGTH,
                message = MemberValidationMessages.NICKNAME_SIZE)
        @Pattern(regexp = MemberValidationPatterns.NICKNAME_PATTERN,
                message = MemberValidationMessages.NICKNAME_PATTERN_INVALID)
        String nickname,

        @NotBlank(message = MemberValidationMessages.EMAIL_REQUIRED)
        @Pattern(regexp = MemberValidationPatterns.EMAIL_PATTERN,
                message = MemberValidationMessages.EMAIL_FORMAT)
        String email,

        @NotNull(message = MemberValidationMessages.SOCIAL_PROVIDER_REQUIRED)
        SocialProvider socialProvider,

        @NotBlank(message = MemberValidationMessages.SOCIAL_ID_REQUIRED)
        @Size(max = MemberValidationConstants.SOCIAL_ID_MAX_LENGTH,
                message = MemberValidationMessages.SOCIAL_ID_SIZE)
        String socialId,

        @EnumValue(enumClass = Gender.class, message = MemberValidationMessages.GENDER_INVALID, allowNull = true)
        String gender,

        @NotNull(message = MemberValidationMessages.BIRTH_REQUIRED)
        @Past(message = MemberValidationMessages.BIRTH_INVALID)
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birth,

        @Size(max = MemberValidationConstants.PHONE_STRING_MAX_LENGTH,
                message = MemberValidationMessages.PHONE_SIZE)
        @Pattern(regexp = MemberValidationPatterns.PHONE_PATTERN,
                message = MemberValidationMessages.PHONE_FORMAT)
        String phone,

        String profileImage
) {
}
