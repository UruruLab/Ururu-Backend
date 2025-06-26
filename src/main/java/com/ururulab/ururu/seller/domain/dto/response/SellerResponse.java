package com.ururulab.ururu.seller.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.seller.domain.entity.Seller;

import java.time.ZonedDateTime;

// 판매자 정보 응답 DTO
// 보안상 민감한 정보(password)는 응답에서 제외
public record SellerResponse(
        Long id,
        String name,
        @JsonProperty("business_name") String businessName,
        @JsonProperty("owner_name") String ownerName,
        @JsonProperty("business_number") String businessNumber,
        String email,
        String phone,
        String image,
        String address1,
        String address2,
        @JsonProperty("mail_order_number") String mailOrderNumber,
        @JsonProperty("created_at") ZonedDateTime createdAt,
        @JsonProperty("updated_at") ZonedDateTime updatedAt,
        @JsonProperty("is_available") Boolean isAvailable
        // password 필드는 보안상 응답에서 제외 (민감한 인증 정보)
) {
    public static SellerResponse of(final Seller seller) {
        if (seller == null) {
            throw new IllegalArgumentException("Seller는 필수입니다.");
        }

        return new SellerResponse(
                seller.getId(),
                seller.getName(),
                seller.getBusinessName(),
                seller.getOwnerName(),
                seller.getBusinessNumber(),
                seller.getEmail(),
                seller.getPhone(),
                seller.getImage(),
                seller.getAddress1(),
                seller.getAddress2(),
                seller.getMailOrderNumber(),
                seller.getCreatedAt(),
                seller.getUpdatedAt(),
                null
        );
    }

    public static SellerResponse ofAvailabilityCheck(boolean isAvailable) {
        return new SellerResponse(
                null, null, null, null, null, null, null, null, null, null, null, null, null, isAvailable
        );
    }
}