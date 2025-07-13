package com.ururulab.ururu.seller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.seller.domain.entity.Seller;

import java.time.Instant;

/**
 * 판매자 정보 응답 DTO
 * 회원가입 완료 및 일반 조회 시 모두 사용
 * 보안상 민감한 정보(password)는 응답에서 제외
 */
public record SellerResponse(
        Long id,
        String name,
        @JsonProperty("business_name") String businessName,
        @JsonProperty("owner_name") String ownerName,
        @JsonProperty("business_number") String businessNumber,
        String email,
        String phone,
        String image,
        String zonecode,
        String address1,
        String address2,
        @JsonProperty("mail_order_number") String mailOrderNumber,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("is_available") Boolean isAvailable
        // password 필드는 보안상 응답에서 제외 (민감한 인증 정보)
) {
    /**
     * 판매자 정보 조회용 응답 생성 (isAvailable 포함)
     * @param seller 판매자 엔티티
     * @return SellerResponse
     */
    public static SellerResponse from(final Seller seller) {
        if (seller == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
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
                seller.getZonecode(),
                seller.getAddress1(),
                seller.getAddress2(),
                seller.getMailOrderNumber(),
                seller.getCreatedAt(),
                seller.getUpdatedAt(),
                !seller.getIsDeleted() // 삭제되지 않은 판매자는 사용 가능
        );
    }

    /**
     * 판매자 회원가입 완료 응답 생성 (isAvailable 제외)
     * @param seller 생성된 판매자 엔티티
     * @return SellerResponse
     */
    public static SellerResponse forSignup(final Seller seller) {
        if (seller == null) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
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
                seller.getZonecode(),
                seller.getAddress1(),
                seller.getAddress2(),
                seller.getMailOrderNumber(),
                seller.getCreatedAt(),
                seller.getUpdatedAt(),
                null // 회원가입 시에는 isAvailable 정보 불필요
        );
    }
}