package com.ururulab.ururu.review.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ururulab.ururu.global.common.entity.Tag;
import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.global.common.repository.TagRepository;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.review.domain.dto.request.ReviewRequest;
import com.ururulab.ururu.review.domain.entity.Review;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;
import com.ururulab.ururu.review.domain.repository.ReviewRepository;
import com.ururulab.ururu.review.event.ReviewCreatedEvent;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ApplicationEventPublisher publisher;
	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	private final ReviewImageService reviewImageService;
	private final TagRepository tagRepository;

	// TODO: CustomException
	// TODO: productRepository 구현 완료 시 product 검증 추가
	// TODO: reviewImageService 구현 완료 시 이미지 저장 처리 추가

	private final static Product product = Product.of(
			"name",
			"description",
			Status.ACTIVE
	);

	@Transactional
	public void createReview(Long memberId, ReviewRequest request) {

		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new EntityNotFoundException("Member not found: " + memberId));

		reviewImageService.validateImages(request.imageFiles());

		Review review = reviewRepository.save(
				Review.ofCreate(
						member,
						product,
						request.productOptionId(),
						request.rating(),
						SkinType.from(request.skinType()),
						AgeGroup.from(request.ageGroup()),
						Gender.from(request.gender()),
						request.content(),
						getTags(request.tags())
				)
		);

		publisher.publishEvent(new ReviewCreatedEvent(review.getId(), request.imageFiles()));
	}

	private List<Tag> getTags(List<Long> tagIds) {
		List<Tag> tags = tagRepository.findAllById(tagIds);
		if (tags.size() != tagIds.size()) {
			throw new IllegalArgumentException("Some tags not found");
		}
		return tags;
	}
}
