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

	private final ReviewImageService imageService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("imageUploadExecutor")
	public void handleReviewCreated(ReviewCreatedEvent event) {
		// 실제 업로드 + DB 저장
		// imageService.uploadAndAssociate(
		// 		event.getReviewId(),
		// 		event.getImages()
		// );
	}
}
