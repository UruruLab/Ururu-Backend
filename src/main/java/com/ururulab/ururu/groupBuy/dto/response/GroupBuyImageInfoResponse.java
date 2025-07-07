package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;

public record GroupBuyImageInfoResponse(
        Long id,
        String imageUrl,
        Integer displayOrder
) {
    public static GroupBuyImageInfoResponse from(GroupBuyImage image) {
        return new GroupBuyImageInfoResponse(
                image.getId(),
                image.getImageUrl(),
                image.getDisplayOrder()
        );
    }
}
