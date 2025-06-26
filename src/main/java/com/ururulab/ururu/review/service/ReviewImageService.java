package com.ururulab.ururu.review.service;

import static com.ururulab.ururu.image.domain.ImageCategory.*;
import static com.ururulab.ururu.review.domain.policy.ReviewImagePolicy.*;

import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.image.domain.ImageFormat;
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
			throw new BusinessException(ErrorCode.REVIEW_IMAGE_COUNT_EXCEEDED, MAX_IMAGE_COUNT);
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
						new BusinessException(ErrorCode.INVALID_IMAGE_FILENAME));
		int idx = filename.lastIndexOf('.');
		if (idx == filename.length() - 1) {
			throw new BusinessException(ErrorCode.INVALID_IMAGE_MIME);
		}
		String ext = filename.substring(idx + 1).toLowerCase();
		return ImageFormat.fromExtension(ext)
				.orElseThrow(() ->
						new BusinessException(
								ErrorCode.IMAGE_FORMAT_MISMATCH,
								file.getOriginalFilename()
						));
	}

	private ImageFormat parseMimeType(MultipartFile file) {
		return Optional.ofNullable(file.getContentType())
				.flatMap(ImageFormat::fromMimeType)
				.orElseThrow(() -> new BusinessException(
						ErrorCode.INVALID_IMAGE_MIME
				));
	}

	private void ensureMatchingFormats(
			ImageFormat extFmt,
			ImageFormat mimeFmt,
			MultipartFile file
	) {
		if (extFmt != mimeFmt) {
			throw new BusinessException(
					ErrorCode.IMAGE_FORMAT_MISMATCH,
					file.getOriginalFilename()
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
				.orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_FILENAME_MISSING));

		List<String> imageUrls = images.stream()
				.map(req -> {
					String filename = Optional.ofNullable(req.originalFilename())
							.orElseThrow(
									() -> new BusinessException(ErrorCode.IMAGE_FILENAME_MISSING));
					return imageService.uploadImage(REVIEWS.getPath(), filename, req.data());
				})
				.toList();

		review.addImages(imageUrls);
	}
}
