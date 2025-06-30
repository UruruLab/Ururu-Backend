package com.ururulab.ururu.seller.domain.dto.request;

import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationConstants;
import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationMessages;
import com.ururulab.ururu.seller.domain.dto.validation.SellerValidationPatterns;
import jakarta.validation.constraints.*;

public record SellerSignupRequest(
        @NotBlank(message = SellerValidationMessages.NAME_REQUIRED)
        @Size(max = SellerValidationConstants.NAME_MAX_LENGTH,
                message = SellerValidationMessages.NAME_SIZE)
        String name,

        @NotBlank(message = SellerValidationMessages.BUSINESS_NAME_REQUIRED)
        @Size(max = SellerValidationConstants.BUSINESS_NAME_MAX_LENGTH,
                message = SellerValidationMessages.BUSINESS_NAME_SIZE)
        String businessName,

        @NotBlank(message = SellerValidationMessages.OWNER_NAME_REQUIRED)
        @Size(max = SellerValidationConstants.OWNER_NAME_MAX_LENGTH,
                message = SellerValidationMessages.OWNER_NAME_SIZE)
        String ownerName,

        @NotBlank(message = SellerValidationMessages.BUSINESS_NUMBER_REQUIRED)
        @Size(min = SellerValidationConstants.BUSINESS_NUMBER_LENGTH,
                max = SellerValidationConstants.BUSINESS_NUMBER_LENGTH,
                message = SellerValidationMessages.BUSINESS_NUMBER_SIZE)
        @Pattern(regexp = SellerValidationPatterns.BUSINESS_NUMBER_PATTERN,
                message = SellerValidationMessages.BUSINESS_NUMBER_PATTERN)
        String businessNumber,

        @NotBlank(message = SellerValidationMessages.EMAIL_REQUIRED)
        @Size(max = SellerValidationConstants.EMAIL_MAX_LENGTH,
                message = SellerValidationMessages.EMAIL_SIZE)
        @Email(message = SellerValidationMessages.EMAIL_FORMAT)
        String email,

        @NotBlank(message = SellerValidationMessages.PASSWORD_REQUIRED)
        @Size(max = SellerValidationConstants.PASSWORD_MAX_LENGTH,
                message = SellerValidationMessages.PASSWORD_SIZE)
        @Pattern(regexp = SellerValidationPatterns.PASSWORD_PATTERN,
                message = SellerValidationMessages.PASSWORD_PATTERN)
        String password,

        @Size(max = SellerValidationConstants.PHONE_MAX_LENGTH,
                message = SellerValidationMessages.PHONE_SIZE)
        @Pattern(regexp = SellerValidationPatterns.PHONE_PATTERN,
                message = SellerValidationMessages.PHONE_FORMAT)
        @NotBlank(message = SellerValidationMessages.PHONE_REQUIRED)
        String phone,

        String image,

        @NotBlank(message = SellerValidationMessages.ADDRESS1_REQUIRED)
        @Size(max = SellerValidationConstants.ADDRESS_MAX_LENGTH,
                message = SellerValidationMessages.ADDRESS1_SIZE)
        String address1,

        @NotBlank(message = SellerValidationMessages.ADDRESS2_REQUIRED)
        @Size(max = SellerValidationConstants.ADDRESS_MAX_LENGTH,
                message = SellerValidationMessages.ADDRESS2_SIZE)
        String address2,

        @NotBlank(message = SellerValidationMessages.MAIL_ORDER_NUMBER_REQUIRED)
        @Size(max = SellerValidationConstants.MAIL_ORDER_NUMBER_MAX_LENGTH,
                message = SellerValidationMessages.MAIL_ORDER_NUMBER_SIZE)
        String mailOrderNumber
) {
} 