package com.ururulab.ururu.groupBuy.dto.request;

public record GroupBuyImageUploadRequest (
        Long groupBuyId,
        Long groupBuyImageId, // 기존 이미지 수정 시
        String originalFilename,
        String tempFilePath,
        Integer displayOrder,
        String detailImageHash
){
}
