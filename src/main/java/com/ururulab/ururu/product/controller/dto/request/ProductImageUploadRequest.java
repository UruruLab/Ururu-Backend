package com.ururulab.ururu.product.controller.dto.request;

public record ProductImageUploadRequest(
        Long productOptionId,
        String originalFilename,
        byte[] data,
        String imageHash
) {
}
