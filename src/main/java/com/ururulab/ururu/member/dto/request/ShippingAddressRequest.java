package com.ururulab.ururu.member.dto.request;

import com.ururulab.ururu.member.dto.validation.ShippingAddressValidationConstants;
import com.ururulab.ururu.member.dto.validation.ShippingAddressValidationMessages;
import com.ururulab.ururu.member.dto.validation.ShippingAddressValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(
        @NotBlank(message = ShippingAddressValidationMessages.LABEL_REQUIRED)
        @Size(min = ShippingAddressValidationConstants.LABEL_MIN_LENGTH,
                max = ShippingAddressValidationConstants.LABEL_MAX_LENGTH,
                message = ShippingAddressValidationMessages.LABEL_SIZE)
        @Pattern(regexp = ShippingAddressValidationPatterns.LABEL_PATTERN,
                message = ShippingAddressValidationMessages.LABEL_PATTERN_INVALID)
        String label,

        @NotBlank(message = ShippingAddressValidationMessages.PHONE_REQUIRED)
        @Size(min = ShippingAddressValidationConstants.PHONE_MIN_LENGTH,
                max = ShippingAddressValidationConstants.PHONE_MAX_LENGTH,
                message = ShippingAddressValidationMessages.PHONE_SIZE)
        @Pattern(regexp = ShippingAddressValidationPatterns.PHONE_PATTERN,
                message = ShippingAddressValidationMessages.PHONE_PATTERN_INVALID)
        String phone,

        @NotBlank(message = ShippingAddressValidationMessages.ZONECODE_REQUIRED)
        @Size(min = ShippingAddressValidationConstants.ZONECODE_LENGTH,
                max = ShippingAddressValidationConstants.ZONECODE_LENGTH,
                message = ShippingAddressValidationMessages.ZONECODE_SIZE)
        @Pattern(regexp = ShippingAddressValidationPatterns.ZONECODE_PATTERN,
                message = ShippingAddressValidationMessages.ZONECODE_PATTERN_INVALID)
        String zonecode,

        @NotBlank(message = ShippingAddressValidationMessages.ADDRESS1_REQUIRED)
        @Size(min = ShippingAddressValidationConstants.ADDRESS1_MIN_LENGTH,
                max = ShippingAddressValidationConstants.ADDRESS1_MAX_LENGTH,
                message = ShippingAddressValidationMessages.ADDRESS1_SIZE)
        String address1,

        @Size(max = ShippingAddressValidationConstants.ADDRESS2_MAX_LENGTH,
                message = ShippingAddressValidationMessages.ADDRESS2_SIZE)
        String address2,

        boolean isDefault
) {
}
