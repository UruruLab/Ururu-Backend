package com.ururulab.ururu.seller.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.seller.domain.entity.Seller;

import java.time.Instant;

// 판매자 회원가입 응답 DTO
// 보안상 민감한 정보(password)는 응답에서 제외
public record SellerSignupResponse(
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
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
        // password 필드는 보안상 응답에서 제외 (민감한 인증 정보)
) {
    public static SellerSignupResponse from(final Seller seller) {
        if (seller == null) {
            throw new IllegalArgumentException("Seller는 필수입니다.");
        }

        return new SellerSignupResponse(
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
                seller.getUpdatedAt()
        );
    }
} 