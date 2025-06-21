package com.ururulab.ururu.review.service;

import java.util.List;

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

		reviewImageService.storeImages(request.imageFiles());
		List<Tag> tags = tagRepository.findAllById(request.tags());

		reviewRepository.save(
				Review.ofCreate(
						member,
						product,
						request.productOptionId(),
						request.rating(),
						SkinType.from(request.skinType()),
						AgeGroup.from(request.ageGroup()),
						Gender.from(request.gender()),
						request.content(),
						tags
				)
		);
	}
}
