package com.ururulab.ururu.groupBuy.event;

public record GroupBuyThumbnailUploadEvent(
        Long groupBuyId,
        String originalFilename,  // 직접 필드
        byte[] data,             // 직접 필드
        String imageHash
) {
}
