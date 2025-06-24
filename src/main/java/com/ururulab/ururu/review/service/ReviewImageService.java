package com.ururulab.ururu.review.service;

import static com.ururulab.ururu.image.domain.ImageCategory.*;
import static com.ururulab.ururu.review.domain.policy.ReviewImagePolicy.*;

import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import com.ururulab.ururu.image.service.ImageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewImageService {

	private final ImageService imageService;

	public void validateImages(List<MultipartFile> images) {
		if (images == null || images.isEmpty()) {
			return;
		}
		if (images.size() > MAX_IMAGE_COUNT) {
			throw new IllegalArgumentException(
					"이미지는 최대 " + MAX_IMAGE_COUNT + "개까지 첨부할 수 있습니다."
			);
		}
		images.forEach(file -> {
			String filename = Optional.ofNullable(file.getOriginalFilename())
					.filter(n -> n.contains("."))
					.orElseThrow(() ->
							new InvalidImageFormatException("파일명이 없거나 확장자를 찾을 수 없습니다.")
					);
			String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
			ImageFormat.fromExtension(ext)
					.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 확장자: " + ext));
		});
	}

	@Async("imageUploadExecutor")
	public void uploadImagesAsync(Long reviewId, List<MultipartFile> images) {
		if (images == null || images.isEmpty()) {
			return;
		}
		images.forEach(file -> {
			try {
				String filename = Optional.ofNullable(file.getOriginalFilename())
						.orElseThrow(() -> new InvalidImageFormatException("파일명이 없습니다."));
				String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
				String url = imageService.uploadImage(
						REVIEWS.getPath(), ext, file
				);
				// TODO: 여기서 url과 reviewId를 이용해 DB에 ReviewImage 엔티티 저장
			} catch (Exception ex) {
				// TODO: 실패 시 로깅 or 재시도 큐에 넣기
			}
		});
	}
}
