package com.ururulab.ururu.groupBuy.domain.dto;

public record GroupBuyDetailImageRequest(
        String originalFilename,
        byte[] data,
        String imageHash,
        int displayOrder
) {
}
