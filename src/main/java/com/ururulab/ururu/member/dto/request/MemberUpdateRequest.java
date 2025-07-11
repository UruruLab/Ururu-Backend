package com.ururulab.ururu.member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.validation.EnumValue;
import com.ururulab.ururu.member.dto.validation.MemberValidationConstants;
import com.ururulab.ururu.member.dto.validation.MemberValidationMessages;
import com.ururulab.ururu.member.dto.validation.MemberValidationPatterns;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record MemberUpdateRequest(
        @Size(min = MemberValidationConstants.NICKNAME_MIN_LENGTH,
                max = MemberValidationConstants.NICKNAME_MAX_LENGTH,
                message = MemberValidationMessages.NICKNAME_SIZE)
        @Pattern(regexp = MemberValidationPatterns.NICKNAME_PATTERN,
                message = MemberValidationMessages.NICKNAME_PATTERN_INVALID)
        String nickname,

        @EnumValue(enumClass = Gender.class,
                message = MemberValidationMessages.GENDER_INVALID,
                allowNull = true)
        String gender,

        @Past(message = MemberValidationMessages.BIRTH_INVALID)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant birth,

        @Size(max = MemberValidationConstants.PHONE_STRING_MAX_LENGTH,
                message = MemberValidationMessages.PHONE_SIZE)
        @Pattern(regexp = MemberValidationPatterns.PHONE_PATTERN,
                message = MemberValidationMessages.PHONE_FORMAT)
        String phone
) {
    /**
     * 업데이트할 필드가 있는지 확인
     */
    public boolean hasUpdates() {
        return nickname != null || gender != null || birth != null || phone != null;
    }

    /**
     * 닉네임 업데이트 여부 확인
     */
    public boolean hasNicknameUpdate() {
        return nickname != null && !nickname.trim().isEmpty();
    }

    /**
     * 성별 업데이트 여부 확인
     */
    public boolean hasGenderUpdate() {
        return gender != null && !gender.trim().isEmpty();
    }

    /**
     * 생년월일 업데이트 여부 확인
     */
    public boolean hasBirthUpdate() {
        return birth != null;
    }

    /**
     * 전화번호 업데이트 여부 확인
     */
    public boolean hasPhoneUpdate() {
        return phone != null && !phone.trim().isEmpty();
    }

}
