package com.ururulab.ururu.image.service;

import com.ururulab.ururu.global.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.Base64;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
public class ImageHashService {
    /**
     * 이미지 해시 계산 (SHA-256)
     */
    public String calculateImageHash(MultipartFile imageFile) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(imageFile.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        }
    }

    /**
     * 이미지 해시 계산 (SHA-256) - byte[]에서
     */
    public String calculateImageHashFromBytes(byte[] imageData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(imageData);
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new BusinessException(IMAGE_PROCESSING_FAILED);
        }
    }
}
