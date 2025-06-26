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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
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

			BufferedImage clean = new BufferedImage(
					img.getWidth(), img.getHeight(),
					BufferedImage.TYPE_INT_ARGB
			);

			Graphics2D g = clean.createGraphics();
			try {
				g.drawImage(img, 0, 0, null);
			} finally {
				g.dispose();
			}

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(clean, fmt.getExtension(), baos);
				return baos.toByteArray();
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
}
