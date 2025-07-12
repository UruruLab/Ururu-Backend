package com.ururulab.ururu.image.service;

import com.ururulab.ururu.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@Slf4j
public class ImageHashService {

    private static final int BUFFER_SIZE = 8192; // 8KB 버퍼

    /**
     * 스트리밍 방식으로 이미지 해시 계산
     */
    public String calculateImageHash(MultipartFile imageFile) {
        try (InputStream inputStream = imageFile.getInputStream()) {
            return calculateHashFromStream(inputStream);
        } catch (IOException e) {
            log.error("Failed to read image file: {}", imageFile.getOriginalFilename(), e);
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        }
    }

    /**
     * 스트림에서 해시 계산
     */
    public String calculateHashFromStream(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return Base64.getEncoder().encodeToString(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        } catch (IOException e) {
            log.error("Failed to read from input stream", e);
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        }
    }
}
