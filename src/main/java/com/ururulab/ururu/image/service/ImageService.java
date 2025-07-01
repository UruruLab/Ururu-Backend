package com.ururulab.ururu.image.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

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

	public String uploadImage(String category, String filename, byte[] data) {
		String ext = Optional.ofNullable(filename)
				.filter(name -> name.contains("."))
				.map(name -> name.substring(name.lastIndexOf('.') + 1).toLowerCase())
				.orElseThrow(() -> new InvalidImageFormatException(
						"파일명 또는 확장자가 유효하지 않습니다: " + filename));

		ImageFormat fmt = ImageFormat.fromExtension(ext)
				.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 이미지 확장자: " + ext));

		byte[] safeData = reencodeImage(data, fmt);

		return uploadToS3(category, fmt, safeData);
	}

	private byte[] reencodeImage(byte[] inputData, ImageFormat fmt) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(inputData));
			if (img == null) {
				throw new InvalidImageFormatException("이미지 파싱 실패");
			}

			int imageType;
			if (fmt.getExtension().equals("jpg") || fmt.getExtension().equals("jpeg")) {
				// JPG는 투명도를 지원하지 않으므로 RGB 사용
				imageType = BufferedImage.TYPE_INT_RGB;
			} else {
				imageType = BufferedImage.TYPE_INT_ARGB;
			}

			BufferedImage clean = new BufferedImage(
					img.getWidth(), img.getHeight(),
					imageType
			);

			Graphics2D g = clean.createGraphics();
			try {
				// JPG의 경우 흰색 배경으로 설정
				if (fmt.getExtension().equals("jpg") || fmt.getExtension().equals("jpeg")) {
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, clean.getWidth(), clean.getHeight());
				}
				g.drawImage(img, 0, 0, null);
			} finally {
				g.dispose();
			}

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				boolean success = ImageIO.write(clean, fmt.getExtension(), baos);
				if (!success) {
					throw new InvalidImageFormatException("이미지 쓰기 실패: " + fmt.getExtension());
				}
				byte[] result = baos.toByteArray();
				return result;
			}
		} catch (IOException e) {
			throw new RuntimeException("이미지 재인코딩 실패", e);
		}
	}

	private String uploadToS3(String category, ImageFormat fmt, byte[] data) {
		String uuid = UUID.randomUUID().toString();
		String key = String.format("%s/%s.%s", category, uuid, fmt.getExtension());

		PutObjectRequest putReq = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(fmt.getMimeType())
				.build();

		try {
			s3Client.putObject(putReq, RequestBody.fromBytes(data));
		} catch (S3Exception e) {
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
	 * 이미지 URL에서 S3 키 추출
	 */
	private String extractKeyFromUrl(String imageUrl) {
		try {
			int productsIndex = imageUrl.indexOf("/products/");
			if (productsIndex == -1) {
				throw new IllegalArgumentException("Invalid image URL format - no products path found");
			}

			return imageUrl.substring(productsIndex + 1); // 맨 앞 "/" 제거

		} catch (Exception e) {
			log.error("Failed to extract S3 key from URL: {}", imageUrl, e);
			throw new IllegalArgumentException("이미지 URL에서 S3 키 추출 실패: " + imageUrl, e);
		}
	}
}
