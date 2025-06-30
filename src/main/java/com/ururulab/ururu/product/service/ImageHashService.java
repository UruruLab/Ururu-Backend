package com.ururulab.ururu.product.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.Base64;

@Service
public class ImageHashService {
    /**
     * 이미지 해시 계산 (MD5)
     */
    public String calculateImageHash(MultipartFile imageFile) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(imageFile.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("이미지 해시 계산 실패: " + e.getMessage(), e);
        }
    }
}
