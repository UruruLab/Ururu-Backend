package com.ururulab.ururu.groupBuy.controller.dto.request;

public record GroupBuyImageUploadRequest (
        Long groupBuyId,
        Long groupBuyImageId, // 기존 이미지 수정 시
        String originalFilename,
        byte[] data,
        Integer displayOrder,
        String detailImageHash
){
}
