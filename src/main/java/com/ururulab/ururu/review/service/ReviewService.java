package com.ururulab.ururu.review.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.review.domain.dto.reqeust.ReviewRequest;
import com.ururulab.ururu.review.domain.entity.Review;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;
import com.ururulab.ururu.review.domain.repository.ReviewRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final MemberRepository memberRepository;
	private final ReviewImageService reviewImageService;

	@Transactional
	public void createReview(Long memberId, ReviewRequest request) {

		// TODO: CustomException
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new EntityNotFoundException("Member not found: " + memberId));

		// TODO: productRepository 구현 완료 시 product 검증 추가
		Product product = Product.of(
				"name",
				"description",
				Status.ACTIVE
		);

		// TODO: reviewImageService 구현 완료 시 이미지 저장 처리 추가
		reviewImageService.storeImages(request.imageFiles());

		reviewRepository.save(
				Review.ofCreate(
						member,
						product,
						request.productOptionId(),
						request.rating(),
						SkinType.from(request.skinType()),
						AgeGroup.from(request.ageGroup()),
						Gender.from(request.gender()),
						request.content()
				)
		);
	}
}
