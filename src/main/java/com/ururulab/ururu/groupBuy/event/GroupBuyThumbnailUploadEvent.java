package com.ururulab.ururu.groupBuy.event;

public record GroupBuyThumbnailUploadEvent(
        Long groupBuyId,
        String originalFilename,
        byte[] data,
        String thumbnailHash
) {
}
