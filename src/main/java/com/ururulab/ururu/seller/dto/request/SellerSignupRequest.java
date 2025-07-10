package com.ururulab.ururu.seller.dto.request;

import com.ururulab.ururu.seller.domain.constant.SellerConstants;
import jakarta.validation.constraints.*;

public record SellerSignupRequest(
        @NotBlank(message = SellerConstants.NAME_REQUIRED)
        @Size(max = SellerConstants.NAME_MAX_LENGTH,
                message = SellerConstants.NAME_SIZE)
        String name,

        @NotBlank(message = SellerConstants.BUSINESS_NAME_REQUIRED)
        @Size(max = SellerConstants.BUSINESS_NAME_MAX_LENGTH,
                message = SellerConstants.BUSINESS_NAME_SIZE)
        String businessName,

        @NotBlank(message = SellerConstants.OWNER_NAME_REQUIRED)
        @Size(max = SellerConstants.OWNER_NAME_MAX_LENGTH,
                message = SellerConstants.OWNER_NAME_SIZE)
        String ownerName,

        @NotBlank(message = SellerConstants.BUSINESS_NUMBER_REQUIRED)
        @Size(min = SellerConstants.BUSINESS_NUMBER_LENGTH,
                max = SellerConstants.BUSINESS_NUMBER_LENGTH,
                message = SellerConstants.BUSINESS_NUMBER_SIZE)
        @Pattern(regexp = SellerConstants.BUSINESS_NUMBER_PATTERN,
                message = SellerConstants.BUSINESS_NUMBER_PATTERN_ERROR)
        String businessNumber,

        @NotBlank(message = SellerConstants.EMAIL_REQUIRED)
        @Size(max = SellerConstants.EMAIL_MAX_LENGTH,
                message = SellerConstants.EMAIL_SIZE)
        @Email(message = SellerConstants.EMAIL_FORMAT)
        String email,

        @NotBlank(message = SellerConstants.PASSWORD_REQUIRED)
        @Size(max = SellerConstants.PASSWORD_MAX_LENGTH,
                message = SellerConstants.PASSWORD_SIZE)
        @Pattern(regexp = SellerConstants.PASSWORD_PATTERN,
                message = SellerConstants.PASSWORD_PATTERN_ERROR)
        String password,

        @Size(max = SellerConstants.PHONE_MAX_LENGTH,
                message = SellerConstants.PHONE_SIZE)
        @Pattern(regexp = SellerConstants.PHONE_PATTERN,
                message = SellerConstants.PHONE_FORMAT)
        @NotBlank(message = SellerConstants.PHONE_REQUIRED)
        String phone,

        String image,

        @NotBlank(message = SellerConstants.ADDRESS1_REQUIRED)
        @Size(max = SellerConstants.ADDRESS_MAX_LENGTH,
                message = SellerConstants.ADDRESS1_SIZE)
        String address1,

        @NotBlank(message = SellerConstants.ADDRESS2_REQUIRED)
        @Size(max = SellerConstants.ADDRESS_MAX_LENGTH,
                message = SellerConstants.ADDRESS2_SIZE)
        String address2,

        @NotBlank(message = SellerConstants.MAIL_ORDER_NUMBER_REQUIRED)
        @Size(max = SellerConstants.MAIL_ORDER_NUMBER_MAX_LENGTH,
                message = SellerConstants.MAIL_ORDER_NUMBER_SIZE)
        String mailOrderNumber
) {
} 