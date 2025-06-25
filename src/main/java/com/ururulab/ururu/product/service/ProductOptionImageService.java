package com.ururulab.ururu.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionImageService {

//    private final S3Client s3Client;
//    private final ProductOptionRepository productOptionRepository;
//
//    private final Set<String> uploadedFileNames = new HashSet<>();
//    private final Set<Long> uploadedFileSizes = new HashSet<>();
//
//    @Value("${spring.cloud.aws.s3.bucket}")
//    private String bucket;
//
//    @Value("${spring.cloud.aws.region.static}")
//    private String region;
//
//    @Value("${spring.cloud.aws.s3.folders.product-option-images}")
//    private String folder;
//
//    /**
//     * 상품 옵션 이미지 단일 업로드
//     */
//    public String uploadProductOptionImage(MultipartFile file) {
//        String randomFilename = generateRandomFilename(file);
//        String fullKey = folder + randomFilename;
//
//        try {
//            PutObjectRequest putRequest = PutObjectRequest.builder()
//                    .bucket(bucket)
//                    .key(fullKey)
//                    .contentType(file.getContentType())
//                    .build();
//
//            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
//            log.info("Product option image uploaded: {}", fullKey);
//
//        } catch (IOException e) {
//            log.error("IO Error while uploading product option image: {}", e.getMessage());
//            throw new RuntimeException("상품 옵션 이미지 업로드 중 IO 오류가 발생했습니다.", e);
//        } catch (S3Exception e) {
//            log.error("S3 Error while uploading product option image: {}", e.getMessage());
//            throw new RuntimeException("상품 옵션 이미지 업로드 중 S3 오류가 발생했습니다.", e);
//        }
//
//        return getFileUrl(fullKey);
//    }
//
//    /**
//     * 상품 옵션의 기존 이미지를 새 이미지로 교체하고 DB 업데이트
//     */
//    @Transactional
//    public String updateProductOptionImage(Long productOptionId, MultipartFile newImageFile) {
//        ProductOption productOption = productOptionRepository.findById(productOptionId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 옵션입니다: " + productOptionId));
//
//        // 기존 이미지가 있다면 삭제
//        if (productOption.getImageUrl() != null && !productOption.getImageUrl().isEmpty()) {
//            deleteProductOptionImage(productOption.getImageUrl());
//        }
//
//        // 새 이미지 업로드
//        String newImageUrl = uploadProductOptionImage(newImageFile);
//
//        // DB에 새 이미지 URL 업데이트
//        productOption.updateImageUrl(newImageUrl);
//        productOptionRepository.save(productOption);
//
//        log.info("Product option image updated for option ID: {}", productOptionId);
//        return newImageUrl;
//    }
//
//    /**
//     * 상품 옵션 이미지 삭제
//     */
//    public void deleteProductOptionImage(String fileUrl) {
//        String key = extractKeyFromUrl(fileUrl);
//
//        try {
//            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
//                    .bucket(bucket)
//                    .key(key)
//                    .build();
//
//            s3Client.deleteObject(deleteRequest);
//            log.info("Product option image deleted: {}", key);
//
//        } catch (S3Exception e) {
//            log.error("S3 deletion failed for product option image: {}", e.getMessage());
//            throw new RuntimeException("상품 옵션 이미지 삭제 중 오류가 발생했습니다.", e);
//        }
//    }
//
//    /**
//     * 상품 옵션 이미지 삭제하고 DB에서 URL 제거
//     */
//    @Transactional
//    public void deleteProductOptionImageByOptionId(Long productOptionId) {
//        ProductOption productOption = productOptionRepository.findById(productOptionId)
//                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품 옵션입니다: " + productOptionId));
//
//        if (productOption.getImageUrl() != null && !productOption.getImageUrl().isEmpty()) {
//            deleteProductOptionImage(productOption.getImageUrl());
//
//            // DB에서 이미지 URL 제거
//            productOption.removeImageUrl();
//            productOptionRepository.save(productOption);
//
//            log.info("Product option image deleted for option ID: {}", productOptionId);
//        }
//    }
//
//    /**
//     * 파일 확장자 검증 (이미지 파일만 허용)
//     */
//    private String validateFileExtension(String originalFilename) {
//        if (originalFilename == null || !originalFilename.contains(".")) {
//            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
//        }
//
//        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
//        List<String> allowed = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
//        if (!allowed.contains(ext)) {
//            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp만 지원)");
//        }
//        return ext;
//    }
//
//    /**
//     * 랜덤 파일명 생성 (가장 빠른 방식)
//     */
//    private String generateRandomFilename(MultipartFile multipartFile) {
//        String ext = validateFileExtension(multipartFile.getOriginalFilename());
//        return UUID.randomUUID().toString() + "." + ext;
//    }
//
//    /**
//     * S3 URL 생성
//     */
//    private String getFileUrl(String key) {
//        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
//    }
//
//    /**
//     * URL에서 S3 키 추출
//     */
//    private String extractKeyFromUrl(String fileUrl) {
//        if (fileUrl == null || fileUrl.isEmpty()) {
//            throw new IllegalArgumentException("파일 URL이 비어있습니다.");
//        }
//
//        String[] parts = fileUrl.split("/");
//        if (parts.length < 4) {
//            throw new IllegalArgumentException("올바르지 않은 S3 URL 형식입니다.");
//        }
//
//        return String.join("/", Arrays.copyOfRange(parts, 3, parts.length));
//    }
//
//    /**
//     * 파일 존재 여부 확인
//     */
//    public boolean doesImageExist(String fileUrl) {
//        try {
//            String key = extractKeyFromUrl(fileUrl);
//            HeadObjectRequest headRequest = HeadObjectRequest.builder()
//                    .bucket(bucket)
//                    .key(key)
//                    .build();
//
//            s3Client.headObject(headRequest);
//            return true;
//        } catch (NoSuchKeyException e) {
//            return false;
//        } catch (S3Exception e) {
//            log.error("Error checking if image exists: {}", e.getMessage());
//            return false;
//        }
//    }
}
