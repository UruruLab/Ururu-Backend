package com.ururulab.ururu.review.domain.dto.request;

public record ImageUploadRequest(
		Long reviewId,
		String originalFilename,
		byte[] data
) {
}
