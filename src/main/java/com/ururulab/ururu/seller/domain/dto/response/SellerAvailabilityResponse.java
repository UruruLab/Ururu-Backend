package com.ururulab.ururu.seller.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// 판매자 가용성 체크 전용 응답 DTO
public record SellerAvailabilityResponse(
        @JsonProperty("is_available") Boolean isAvailable,
        @JsonProperty("message") String message
) {
    public static SellerAvailabilityResponse of(boolean isAvailable) {
        String message = isAvailable ? "사용 가능합니다." : "이미 사용 중입니다.";
        return new SellerAvailabilityResponse(isAvailable, message);
    }

    public static SellerAvailabilityResponse ofEmailAvailability(boolean isAvailable) {
        String message = isAvailable ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.";
        return new SellerAvailabilityResponse(isAvailable, message);
    }

    public static SellerAvailabilityResponse ofBusinessNumberAvailability(boolean isAvailable) {
        String message = isAvailable ? "사용 가능한 사업자등록번호입니다." : "이미 사용 중인 사업자등록번호입니다.";
        return new SellerAvailabilityResponse(isAvailable, message);
    }

    public static SellerAvailabilityResponse ofNameAvailability(boolean isAvailable) {
        String message = isAvailable ? "사용 가능한 브랜드명입니다." : "이미 사용 중인 브랜드명입니다.";
        return new SellerAvailabilityResponse(isAvailable, message);
    }
} 