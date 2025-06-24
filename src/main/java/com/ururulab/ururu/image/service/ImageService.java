package com.ururulab.ururu.image.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class ImageService {

	private final S3Client s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public String uploadImage(String category, String extension, MultipartFile file) {
		try {
			ImageFormat fmt = validateFormat(extension, file);
			byte[] safeData = decodeAndReEncode(file, fmt);
			return uploadToS3(category, fmt, safeData);
		} catch (IOException e) {
			throw new RuntimeException("이미지 업로드 실패", e);
		}
	}

	private ImageFormat validateFormat(String extension, MultipartFile file) {
		ImageFormat extFmt = ImageFormat.fromExtension(extension)
				.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 확장자: " + extension));

		String mime = Optional.ofNullable(file.getContentType())
				.orElseThrow(() -> new InvalidImageFormatException("MIME 타입 확인 불가"));
		ImageFormat mimeFmt = ImageFormat.fromMimeType(mime)
				.orElseThrow(() -> new InvalidImageFormatException("지원하지 않는 MIME 타입: " + mime));

		if (extFmt != mimeFmt) {
			throw new InvalidImageFormatException(
					String.format("확장자(%s)와 MIME(%s) 불일치", extension, mime));
		}
		return extFmt;
	}

	private byte[] decodeAndReEncode(MultipartFile file, ImageFormat fmt) throws IOException {
		BufferedImage img = ImageIO.read(file.getInputStream());
		if (img == null) {
			throw new InvalidImageFormatException("이미지 파싱 실패");
		}
		BufferedImage clean = new BufferedImage(img.getWidth(), img.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		clean.getGraphics().drawImage(img, 0, 0, null);

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(clean, fmt.getExtension(), baos);
			return baos.toByteArray();
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

		s3Client.putObject(putReq, RequestBody.fromBytes(data));

		URL url = s3Client.utilities()
				.getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build());
		return url.toString();
	}
}
