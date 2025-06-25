package com.ururulab.ururu.review.event;

import java.util.List;

import com.ururulab.ururu.review.domain.dto.request.ImageUploadRequest;

public class ReviewCreatedEvent {
	private final Long reviewId;
	private final List<ImageUploadRequest> images;

	public ReviewCreatedEvent(Long reviewId, List<ImageUploadRequest> images) {
		this.reviewId = reviewId;
		this.images = images;
	}

	public Long getReviewId() {
		return reviewId;
	}

	public List<ImageUploadRequest> getImages() {
		return images;
	}
}
