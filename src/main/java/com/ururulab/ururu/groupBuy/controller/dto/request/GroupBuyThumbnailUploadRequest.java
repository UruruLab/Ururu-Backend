package com.ururulab.ururu.groupBuy.controller.dto.request;

public record GroupBuyThumbnailUploadRequest (
        Long groupBuyId,
        String originalFilename,
        byte[] data,
        String thumbNailHash
){
}
