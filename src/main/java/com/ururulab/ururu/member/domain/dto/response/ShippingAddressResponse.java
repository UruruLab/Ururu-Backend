package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.ShippingAddress;

import java.time.ZonedDateTime;

public record ShippingAddressResponse(
        Long id,
        String label,
        String phone,
        String zonecode,
        String address1,
        String address2,
        @JsonProperty("is_default") boolean isDefault,
        @JsonProperty("created_at") ZonedDateTime createdAt,
        @JsonProperty("updated_at") ZonedDateTime updatedAt
) {
    public static ShippingAddressResponse from(final ShippingAddress shippingAddress) {
        return new ShippingAddressResponse(
                shippingAddress.getId(),
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
