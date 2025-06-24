package com.ururulab.ururu.review.event;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ReviewCreatedEvent {
	private final Long reviewId;
	private final List<MultipartFile> images;

	public ReviewCreatedEvent(Long reviewId, List<MultipartFile> images) {
		this.reviewId = reviewId;
		this.images = images;
	}

	public Long getReviewId() {
		return reviewId;
	}

	public List<MultipartFile> getImages() {
		return images;
	}
}
