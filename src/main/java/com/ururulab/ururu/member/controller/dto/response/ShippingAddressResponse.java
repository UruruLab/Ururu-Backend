package com.ururulab.ururu.member.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;

import java.time.Instant;

public record ShippingAddressResponse(
        Long id,
        @JsonProperty("member_id") Long memberId,
        String label,
        String phone,
        String zonecode,
        String address1,
        String address2,
        @JsonProperty("is_default") boolean isDefault,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static ShippingAddressResponse from(final ShippingAddress shippingAddress) {
        return new ShippingAddressResponse(
                shippingAddress.getId(),
                shippingAddress.getMember().getId(),
                shippingAddress.getLabel(),
                shippingAddress.getPhone(),
                shippingAddress.getZonecode(),
                shippingAddress.getAddress1(),
                shippingAddress.getAddress2(),
                shippingAddress.isDefault(),
                shippingAddress.getCreatedAt(),
                shippingAddress.getUpdatedAt()
        );
    }
}
