package com.ururulab.ururu.review.service;

import com.ururulab.ururu.global.domain.entity.Tag;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.global.domain.repository.TagRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.review.domain.dto.request.ImageUploadRequest;
import com.ururulab.ururu.review.domain.dto.request.ReviewRequest;
import com.ururulab.ururu.review.domain.entity.Review;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;
import com.ururulab.ururu.review.domain.repository.ReviewRepository;
import com.ururulab.ururu.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.IMAGE_CONVERSION_FAILED;
import static com.ururulab.ururu.global.exception.error.ErrorCode.TAG_NOT_FOUND;

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

//	private final static Product product = Product.of(
//			"name",
//			"description",
//			Status.ACTIVE
//	);

	private static final Member member = Member.of(
			"nickname",
			"email",
			SocialProvider.KAKAO,
			"socialId",
			Gender.MALE,
			LocalDate.now(),
			"phone",
			"profileImage",
			Role.NORMAL
	);

	@Transactional
	public void createReview(Long memberId, ReviewRequest request, List<MultipartFile> imageFiles) {

		// Member member = memberRepository.findById(memberId)
		// 		.orElseThrow(() -> new EntityNotFoundException("Member not found: " + memberId));

		reviewImageService.validateImages(imageFiles);

		Review review = reviewRepository.save(
				Review.ofCreate(
						// member,
						// product,
						request.productOptionId(),
						request.rating(),
						SkinType.from(request.skinType()),
						AgeGroup.from(request.ageGroup()),
						Gender.from(request.gender()),
						request.content()
						// getTags(request.tags())
				)
		);

		List<ImageUploadRequest> uploads = toImageUploadRequests(review.getId(), imageFiles);

		publisher.publishEvent(
				new ReviewCreatedEvent(review.getId(), uploads)
		);
	}

	private List<Tag> getTags(List<Long> tagIds) {
		List<Tag> tags = tagRepository.findAllById(tagIds);
		if (tags.size() != tagIds.size()) {
			throw new BusinessException(TAG_NOT_FOUND);
		}
		return tags;
	}

	private List<ImageUploadRequest> toImageUploadRequests(Long reviewId, List<MultipartFile> files) {
		return files.stream()
				.map(file -> {
					try {
						return new ImageUploadRequest(
								reviewId,
								file.getOriginalFilename(),
								file.getBytes()
						);
					} catch (IOException e) {
						throw new BusinessException(IMAGE_CONVERSION_FAILED);
					}
				})
				.toList();
	}

}
