package com.ururulab.ururu.product.dto.request;

public record ProductImageUploadRequest(
        Long productOptionId,
        String originalFilename,
        String tempFilePath,
        String imageHash
) {
}
