package com.ururulab.ururu.groupBuy.domain.dto.request;

public record GroupBuyThumbnailUploadRequest (
        Long groupBuyId,
        String originalFilename,
        byte[] data,
        String thumbNailHash
){
}
