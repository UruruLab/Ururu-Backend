package com.ururulab.ururu.review.controller;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.review.domain.dto.request.ReviewRequest;
import com.ururulab.ururu.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;
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

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> createReview(
			@RequestPart(value = "review") @Valid ReviewRequest reviewRequest,
			@RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles
	) {
		reviewService.createReview(member.getId(), reviewRequest, imageFiles);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
}
