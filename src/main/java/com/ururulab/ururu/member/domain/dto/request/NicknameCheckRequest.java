package com.ururulab.ururu.member.domain.dto.request;

import com.ururulab.ururu.member.domain.dto.validation.MemberValidationConstants;
import com.ururulab.ururu.member.domain.dto.validation.MemberValidationMessages;
import com.ururulab.ururu.member.domain.dto.validation.MemberValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NicknameCheckRequest(
        @NotBlank(message = MemberValidationMessages.NICKNAME_REQUIRED)
        @Size(min = MemberValidationConstants.NICKNAME_MIN_LENGTH,
                max = MemberValidationConstants.NICKNAME_MAX_LENGTH,
                message = MemberValidationMessages.NICKNAME_SIZE)
        @Pattern(regexp = MemberValidationPatterns.NICKNAME_PATTERN,
                message = MemberValidationMessages.NICKNAME_PATTERN_INVALID)
        String nickname
) {
}
