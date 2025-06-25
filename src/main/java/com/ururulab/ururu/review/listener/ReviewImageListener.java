package com.ururulab.ururu.review.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ururulab.ururu.review.event.ReviewCreatedEvent;
import com.ururulab.ururu.review.service.ReviewImageService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewImageListener {

	private final ReviewImageService reviewImageService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("imageUploadExecutor")
	public void handleReviewCreated(ReviewCreatedEvent event) {
		reviewImageService.uploadImagesAsync(
				event.getReviewId(),
				event.getImages()
		);
	}
}
