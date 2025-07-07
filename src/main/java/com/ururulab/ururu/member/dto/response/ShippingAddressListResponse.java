package com.ururulab.ururu.member.dto.response;

import java.util.List;

public record ShippingAddressListResponse(
        List<ShippingAddressResponse> addresses
) {
    public static ShippingAddressListResponse from(final List<ShippingAddressResponse> addresses) {
        return new ShippingAddressListResponse(addresses);
    }
}
