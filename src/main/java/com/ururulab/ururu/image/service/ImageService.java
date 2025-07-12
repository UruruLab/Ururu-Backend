package com.ururulab.ururu.image.service;

import java.io.*;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import com.ururulab.ururu.image.domain.ImageCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

	private final S3Client s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	/**
	 * File 스트리밍 업로드 (임시 파일용)
	 */
	public String uploadFileStreaming(File file, String originalFilename, String category) {
		String ext = extractExtensionFromFilename(originalFilename);
		ImageFormat fmt = ImageFormat.fromExtension(ext)
				.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 이미지 확장자: " + ext));

		try (InputStream inputStream = new java.io.FileInputStream(file)) {
			return uploadStreamToS3(category, fmt, inputStream, file.length());
		} catch (Exception e) {
			log.error("Failed to upload file: {}", originalFilename, e);
			throw new InvalidImageFormatException("파일 업로드 실패: " + e.getMessage());
		}
	}

	/**
	 * S3 스트리밍 업로드
	 */
	private String uploadStreamToS3(String category, ImageFormat fmt, InputStream inputStream, long contentLength) {
		String uuid = UUID.randomUUID().toString();
		String key = String.format("%s/%s.%s", category, uuid, fmt.getExtension());

		PutObjectRequest putReq = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(fmt.getMimeType())
				.contentLength(contentLength)
				.build();

		try {
			s3Client.putObject(putReq, RequestBody.fromInputStream(inputStream, contentLength));
			log.info("Successfully uploaded image to S3: {}", key);
		} catch (S3Exception e) {
			log.error("S3 upload failed for key: {}", key, e);
			throw new InvalidImageFormatException("S3 업로드 실패: " + e.getMessage());
		}

		URL url = s3Client.utilities()
				.getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build());
		return url.toString();
	}

	/**
	 * S3에서 이미지 삭제
	 */
	public void deleteImage(String imageUrl) {
		try {
			String key = extractKeyFromUrl(imageUrl);

			DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.build();

			s3Client.deleteObject(deleteReq);
			log.info("Successfully deleted image from S3: {}", key);
		} catch (S3Exception e) {
			log.error("S3 deletion failed for URL: {}", imageUrl, e);
			throw new RuntimeException("S3 이미지 삭제 실패: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Failed to delete image: {}", imageUrl, e);
			throw new RuntimeException("이미지 삭제 실패", e);
		}
	}

	/**
	 * 파일명에서 확장자 추출
	 */
	private String extractExtensionFromFilename(String filename) {
		return Optional.ofNullable(filename)
				.filter(name -> name.contains("."))
				.map(name -> name.substring(name.lastIndexOf('.') + 1).toLowerCase())
				.orElseThrow(() -> new InvalidImageFormatException(
						"파일명 또는 확장자가 유효하지 않습니다: " + filename));
	}

	/**
	 * 이미지 URL에서 S3 키 추출
	 */
	private String extractKeyFromUrl(String imageUrl) {
		try {
			for (ImageCategory category : ImageCategory.values()) {
				String path = "/" + category.getPath() + "/";
				int pathIndex = imageUrl.indexOf(path);
				if (pathIndex != -1) {
					return imageUrl.substring(pathIndex + 1);
				}
			}
			throw new IllegalArgumentException("Invalid image URL format - no path found");
		} catch (Exception e) {
			log.error("Failed to extract S3 key from URL: {}", imageUrl, e);
			throw new IllegalArgumentException("이미지 URL에서 S3 키 추출 실패: " + imageUrl, e);
		}
	}
}
