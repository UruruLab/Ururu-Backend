package com.ururulab.ururu.image.validation;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.image.domain.ImageFormat;
import com.ururulab.ururu.image.exception.InvalidImageFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Component
@Slf4j
public class ImageValidator {

    @Value("${spring.servlet.multipart.max-file-size:8MB}")
    private String maxFileSize;

    @Value("${spring.servlet.multipart.max-request-size:40MB}")
    private String maxRequestSize;

    /**
     *  공동구매 이미지 검증
     */
    public void validateImage(MultipartFile image) {
        long start = System.currentTimeMillis();

        if (image == null || image.isEmpty()) {
            return;
        }

        ImageFormat extFmt = parseExtension(image);
        ImageFormat mimeFmt = parseMimeType(image);
        ensureMatchingFormats(extFmt, mimeFmt, image);
        ensureContentSignature(extFmt, image);

        long end = System.currentTimeMillis();
        log.info("Image validation took: {}ms for file: {}", end - start, image.getOriginalFilename());
    }

    private void ensureContentSignature(ImageFormat format, MultipartFile file) {
        byte[] header = new byte[12];
        try (InputStream input = file.getInputStream()) {
            if (input.readNBytes(header, 0, header.length) < header.length) {
                throw new InvalidImageFormatException("이미지 파일이 손상되었습니다.");
            }
        } catch (IOException e) {
            throw new InvalidImageFormatException("이미지 파일을 읽을 수 없습니다.");
        }

        boolean valid = switch (format) {
            case JPG, JPEG -> (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            case PNG -> (header[0] & 0xff) == 0x89 && "PNG".equals(new String(header, 1, 3, StandardCharsets.US_ASCII));
            case WEBP -> "RIFF".equals(new String(header, 0, 4, StandardCharsets.US_ASCII))
                    && "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII));
        };
        if (!valid) {
            throw new InvalidImageFormatException("파일 내용과 이미지 형식이 일치하지 않습니다.");
        }
    }

    public void validateAllImages(List<MultipartFile> optionImages) {
        if (optionImages == null || optionImages.isEmpty()) {
            return;
        }

        for (int i = 0; i < optionImages.size(); i++) {
            MultipartFile imageFile = optionImages.get(i);

            if (imageFile == null || imageFile.isEmpty()) {
                continue;
            }

            try {
                validateImage(imageFile);  // 기존 메서드 재사용
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        String.format("옵션 %d번째 이미지가 유효하지 않습니다: %s", i + 1, e.getMessage()), e
                );
            }
        }
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
        if (!extFmt.getMimeType().equals(mimeFmt.getMimeType())) {
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

    /**
     * 단일 파일 크기 검증
     */
    public void validateSingleFileSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        long maxFileSizeBytes = parseSize(maxFileSize);

        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessException(IMAGE_SIZE_EXCEEDED,
                    String.format("파일 '%s'의 크기가 %s를 초과했습니다. (실제: %.1fMB)",
                            file.getOriginalFilename(), maxFileSize, file.getSize() / 1024.0 / 1024.0));
        }

        log.debug("Single file size validation passed: {} ({}MB)",
                file.getOriginalFilename(), file.getSize() / 1024.0 / 1024.0);
    }


    /**
     * 파일 크기 검증 (개별 파일 및 전체 요청 크기)
     */
    public void validateFileSizes(List<MultipartFile> files) {
        long maxFileSizeBytes = parseSize(maxFileSize);
        long maxRequestSizeBytes = parseSize(maxRequestSize);
        long totalSize = 0;

        for (MultipartFile file : files) {
            if (file.getSize() > maxFileSizeBytes) {
                throw new BusinessException(IMAGE_SIZE_EXCEEDED,
                        String.format("파일 '%s'의 크기가 %s를 초과했습니다. (실제: %.1fMB)",
                                file.getOriginalFilename(), maxFileSize, file.getSize() / 1024.0 / 1024.0));
            }
            totalSize += file.getSize();
        }

        if (totalSize > maxRequestSizeBytes) {
            throw new BusinessException(REQUEST_TOO_LARGE,
                    String.format("전체 요청 크기가 %s를 초과했습니다. (실제: %.1fMB)",
                            maxRequestSize, totalSize / 1024.0 / 1024.0));
        }
    }

    /**
     * 크기 문자열을 바이트로 변환 (예: "8MB" -> 8388608)
     */
    private long parseSize(String sizeStr) {
        try{
            sizeStr = sizeStr.toUpperCase().trim();
            if (sizeStr.endsWith("MB")) {
                double value = Double.parseDouble(sizeStr.replace("MB", "").trim());
                if (value < 0) {
                    throw new IllegalArgumentException("크기는 음수일 수 없습니다: " + sizeStr);
                    }
                return (long) (value * 1024 * 1024);
            } else if (sizeStr.endsWith("KB")) {
                double value = Double.parseDouble(sizeStr.replace("KB", "").trim());
                if (value < 0) {
                    throw new IllegalArgumentException("크기는 음수일 수 없습니다: " + sizeStr);
                    }
                return (long) (value * 1024);
            } else if (sizeStr.endsWith("GB")) {
                double value = Double.parseDouble(sizeStr.replace("GB", "").trim());
                if (value < 0) {
                    throw new IllegalArgumentException("크기는 음수일 수 없습니다: " + sizeStr);
                }
                return (long) (value * 1024 * 1024 * 1024);
            } else {
                long value = Long.parseLong(sizeStr);
                if (value < 0) {
                    throw new IllegalArgumentException("크기는 음수일 수 없습니다: " + sizeStr);
                    }
                return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("잘못된 크기 형식: " + sizeStr, e);
        }

    }
}
