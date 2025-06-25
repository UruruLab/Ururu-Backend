package com.ururulab.ururu.review.service;

import static com.ururulab.ururu.image.domain.ImageCategory.*;
import static com.ururulab.ururu.review.domain.policy.ReviewImagePolicy.*;

import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.review.domain.dto.request.ImageUploadRequest;
import com.ururulab.ururu.review.domain.entity.Review;
import com.ururulab.ururu.review.domain.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewImageService {

	private final ImageService imageService;
	private final ReviewRepository reviewRepository;

	public void validateImages(List<MultipartFile> images) {
		if (images == null || images.isEmpty()) {
			return;
		}
		if (images.size() > MAX_IMAGE_COUNT) {
			throw new IllegalArgumentException(
					"이미지는 최대 " + MAX_IMAGE_COUNT + "개까지 첨부할 수 있습니다."
			);
		}
		images.forEach(this::validateSingleImage);
	}

	private void validateSingleImage(MultipartFile file) {
		ImageFormat extFmt = parseExtension(file);

		ImageFormat mimeFmt = parseMimeType(file);

		ensureMatchingFormats(extFmt, mimeFmt, file);
	}

	private ImageFormat parseExtension(MultipartFile file) {
		String filename = Optional.ofNullable(file.getOriginalFilename())
				.filter(n -> n.contains("."))
				.orElseThrow(() ->
						new InvalidImageFormatException("파일명이 없거나 확장자를 찾을 수 없습니다.")
				);
		int idx = filename.lastIndexOf('.');
		if (idx == filename.length() - 1) {
			throw new InvalidImageFormatException("파일명이 마침표로 끝납니다: " + filename);
		}
		String ext = filename.substring(idx + 1).toLowerCase();
		return ImageFormat.fromExtension(ext)
				.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 확장자: " + ext));
	}

	private ImageFormat parseMimeType(MultipartFile file) {
		String mime = Optional.ofNullable(file.getContentType())
				.orElseThrow(() ->
						new InvalidImageFormatException("MIME 타입을 확인할 수 없습니다.")
				);
		return ImageFormat.fromMimeType(mime)
				.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 MIME 타입: " + mime));
	}

	private void ensureMatchingFormats(
			ImageFormat extFmt,
			ImageFormat mimeFmt,
			MultipartFile file
	) {
		if (extFmt != mimeFmt) {
			throw new InvalidImageFormatException(
					String.format(
							"확장자(%s)와 MIME(%s)이 일치하지 않습니다: file=%s",
							extFmt.getExtension(),
							mimeFmt.getMimeType(),
							file.getOriginalFilename()
					)
			);
		}
	}

	@Async("imageUploadExecutor")
	@Transactional
	public void uploadImagesAsync(Long reviewId, List<ImageUploadRequest> images) {
		if (images == null || images.isEmpty()) {
			return;
		}

		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new IllegalArgumentException("review가 존재하지 않습니다."));

		List<String> imageUrls = images.stream()
				.map(req -> {
					String filename = Optional.ofNullable(req.originalFilename())
							.orElseThrow(() -> new IllegalArgumentException("파일명이 없습니다."));
					return imageService.uploadImage(REVIEWS.getPath(), filename, req.data());
				})
				.toList();

		review.addImages(imageUrls);
	}
}
