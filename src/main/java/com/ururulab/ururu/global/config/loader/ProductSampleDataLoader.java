package com.ururulab.ururu.global.config.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.image.service.ImageHashService;
import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.*;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class ProductSampleDataLoader implements CommandLineRunner{

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final ObjectMapper objectMapper;
    private final ImageService imageService;
    private final ImageHashService imageHashService;

    private static final Map<String, Long> CATEGORY_MAPPING = new HashMap<>();

    static {
        // 메이크업
        CATEGORY_MAPPING.put("메이크업-립", 56L);
        CATEGORY_MAPPING.put("메이크업-베이스", 63L);
        CATEGORY_MAPPING.put("메이크업-아이", 74L);

        // 스킨케어
        CATEGORY_MAPPING.put("스킨-더모코스메틱", 2L);
        CATEGORY_MAPPING.put("스킨-크림", 5L);

        // 선케어
        CATEGORY_MAPPING.put("선케어-선크림", 44L);

        // 기본값
        CATEGORY_MAPPING.put("DEFAULT", 1L);
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            log.info("Product sample data already exists (count: {}). Skipping data loading.",
                    productRepository.count());
            return;
        }

        log.info("🚀 Starting to load product sample data from JSON...");
        loadSampleData();
        log.info("✅ Product sample data loading completed!");
    }

    @Transactional
    public void loadSampleData() {
        List<String> jsonFiles = Arrays.asList(
                "/data/makeupLip.json",
                "/data/makeupBase.json",
                "/data/makeupEye.json",
                "/data/skincareCream.json",
                "/data/skincareSkin.json",
                "/data/suncareSuncream.json"
        );

        Seller seller = getDefaultSeller();
        if (seller == null) {
            log.error("셀러 기본값이 없습니다. 셀러 데이터를 먼저 생성해주세요.");
            return;
        }

        int totalSuccessCount = 0;
        int totalErrorCount = 0;
        int totalOptionsCreated = 0;
        int totalProductsProcessed = 0;

        for (String filePath : jsonFiles) {
            try {
                log.debug("📂 Processing file: {}", filePath);

                InputStream inputStream = getClass().getResourceAsStream(filePath);
                if (inputStream == null) {
                    log.error("Sample data file not found: {}", filePath);
                    continue;
                }

                List<Map<String, Object>> productDataList = objectMapper.readValue(
                        inputStream, new TypeReference<List<Map<String, Object>>>() {}
                );

                log.debug("Found {} products in sample data file", productDataList.size());

                int fileSuccessCount = 0;
                int fileErrorCount = 0;
                int fileOptionCreated = 0;

                for (int i = 0; i < productDataList.size(); i++) {
                    Map<String, Object> productData = productDataList.get(i);
                    try {
                        int optionCreated = processProductData(productData, seller);
                        fileOptionCreated += optionCreated;
                        fileSuccessCount++;
                        totalProductsProcessed++;

                        if ((i + 1) % 10 == 0) {
                            log.debug("Progress: {}/{} products processed", i + 1, productDataList.size());
                        }
                    } catch (Exception e) {
                        fileErrorCount++;
                        log.error("Failed to process product: {} - Error: {}", productData.get("prd_name"), e.getMessage());
                    }
                }

                totalSuccessCount += fileSuccessCount;
                totalErrorCount += fileErrorCount;
                totalOptionsCreated += fileOptionCreated;

                log.info("Data loading completed! Success: {}, Errors: {}, Total: {}",
                        fileSuccessCount, fileErrorCount, productDataList.size());
            } catch (Exception e) {
                throw new RuntimeException("Sample Data Loading failed", e);
            }
        }
    }

    /**
     * 스트리밍 방식으로 이미지 다운로드 및 업로드 (메모리 효율적)
     * 기존 byte[] 방식에서 MultipartFile + 스트리밍 방식으로 변경됨
     */
    @Async("imageUploadExecutor")
    public void downloadAndUploadImages(Long productId, List<ProductOption> options, String imageUrl) {
        File tempFile = null;
        try {
            // URL에서 임시 파일로 다운로드 (메모리에 로드하지 않음 - 기존 byte[] 방식 대체)
            tempFile = downloadImageToTempFile(imageUrl);
            if (tempFile == null || !tempFile.exists()) {
                log.debug("Failed to download image from URL: {}", imageUrl);
                return;
            }

            // 임시 파일에서 직접 해시 계산 (스트리밍 방식 - 기존 calculateImageHashFromBytes 대체)
            String imageHash = calculateImageHashFromFile(tempFile);

            // 스트리밍 방식으로 S3 업로드 (기존 uploadImage(byte[]) 대체)
            String fileName = extractFileNameFromUrl(imageUrl);
            String uploadImageUrl = imageService.uploadFileStreaming(
                    tempFile,
                    fileName,
                    "products"
            );

            // DB 업데이트
            for (ProductOption option : options) {
                option.updateImageInfo(uploadImageUrl, imageHash);
                productOptionRepository.save(option);
            }

            log.info("Successfully uploaded image for product: {} -> {}", productId, uploadImageUrl);

        } catch (Exception e) {
            log.error("Failed to download and upload image for product: {}, URL: {}, Error: {}",
                    productId, imageUrl, e.getMessage(), e);
        } finally {
            // 임시 파일 정리
            cleanupTempFile(tempFile);
        }
    }

    /**
     * URL에서 임시 파일로 다운로드 (스트리밍 방식 - 기존 downloadImageFromUrl byte[] 방식 대체)
     */
    private File downloadImageToTempFile(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String fileName = extractFileNameFromUrl(imageUrl);

            // 임시 파일 생성
            File tempFile = Files.createTempFile(
                    "sample_" + System.currentTimeMillis() + "_",
                    "_" + fileName
            ).toFile();
            tempFile.deleteOnExit();

            // 스트리밍 방식으로 다운로드
            try (InputStream inputStream = url.openStream()) {
                Files.copy(inputStream, tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                log.debug("Downloaded {} bytes from URL: {} to temp file: {}",
                        tempFile.length(), imageUrl, tempFile.getName());
                return tempFile;
            }

        } catch (Exception e) {
            log.error("Failed to download image from URL: {}, Error: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 파일에서 직접 해시 계산 (스트리밍 방식 - 기존 calculateImageHashFromBytes 대체)
     */
    private String calculateImageHashFromFile(File imageFile) {
        try (InputStream inputStream = Files.newInputStream(imageFile.toPath())) {
            return imageHashService.calculateHashFromStream(inputStream);
        } catch (Exception e) {
            log.error("Failed to calculate hash from file: {}", imageFile.getName(), e);
            throw new RuntimeException("이미지 해시 계산 실패", e);
        }
    }

    /**
     * 임시 파일 정리
     */
    private void cleanupTempFile(File tempFile) {
        try {
            if (tempFile != null && tempFile.exists() && tempFile.delete()) {
                log.debug("Cleaned up temp file: {}", tempFile.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp file: {}", tempFile != null ? tempFile.getName() : "null", e);
        }
    }

    private String extractFileNameFromUrl(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }

            if (!fileName.contains(".")) {
                fileName += ".jpg";
            }

            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            fileName = nameWithoutExt + "_" + System.currentTimeMillis() + extension;

            log.debug("Extracted filename: {} from URL: {}", fileName, imageUrl);
            return fileName;

        } catch (Exception e) {
            log.warn("Failed to extract filename from URL: {}, using default", imageUrl);
            return "product_image_" + System.currentTimeMillis() + ".jpg";
        }
    }

    private int processProductData(Map<String, Object> data, Seller seller) {
        Product product = createProduct(data, seller);
        Product savedProduct = productRepository.save(product);

        ProductNotice productNotice = createProductNotice(data, savedProduct);
        productNoticeRepository.save(productNotice);

        List<ProductOptionInfo> optionInfos = parseProductOptions(data);
        List<ProductOption> savedOptions = new ArrayList<>();

        for (ProductOptionInfo optionInfo : optionInfos) {
            ProductOption productOption = createProductOption(data, savedProduct, optionInfo);
            savedOptions.add(productOptionRepository.save(productOption));
        }

        log.debug("📂 Linking categories for product: '{}'", savedProduct.getName());
        linkProductCategory(data, savedProduct);

        long categoryCount = productCategoryRepository.findByProductId(savedProduct.getId()).size();
        log.info("📊 Product '{}' now has {} categories linked", savedProduct.getName(), categoryCount);

        String imageUrl = (String) data.get("img_url");
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            downloadAndUploadImages(savedProduct.getId(), savedOptions, imageUrl);
        }
        return savedOptions.size();
    }

    private List<ProductOptionInfo> parseProductOptions(Map<String, Object> data) {
        String ingredients = (String) data.get("ingredients");
        if (ingredients == null || ingredients.trim().isEmpty()) {
            return Collections.singletonList(new ProductOptionInfo("기본 옵션", "성분 정보가 제공되지 않습니다."));
        }

        List<ProductOptionInfo> options = new ArrayList<>();

        if (ingredients.contains("[") && ingredients.contains("]")) {
            Pattern optionPattern = Pattern.compile("\\[([^\\]]+)\\]([^\\[]+?)(?=\\[|$)");
            Matcher matcher = optionPattern.matcher(ingredients);

            while (matcher.find()) {
                String optionName = matcher.group(1).trim();
                String optionIngredients = matcher.group(2).trim();

                optionIngredients = cleanIngredients(optionIngredients);

                if (!optionName.isEmpty() && !optionIngredients.isEmpty()) {
                    options.add(new ProductOptionInfo(optionName, optionIngredients));
                    log.debug("Parsed option: [{}] - ingredients length: {}", optionName, optionIngredients.length());
                }
            }
        }

        if (options.isEmpty()) {
            String cleanedIngredients = cleanIngredients(ingredients);
            options.add(new ProductOptionInfo("단일 옵션", cleanedIngredients));
            log.debug("No options parsed, created single option with full ingredients");
        }

        return options;
    }

    private String cleanIngredients(String ingredients) {
        if (ingredients == null) return "";

        return ingredients
                .replaceAll("\\s+", " ")
                .replaceAll("^[,\\s]+", "")
                .replaceAll("[,\\s]+$", "")
                .trim();
    }

    private Product createProduct(Map<String, Object> data, Seller seller) {
        String productName = cleanProductName((String) data.get("prd_name"));
        String description = (String) data.getOrDefault("specifications",
                (String) data.getOrDefault("usage_instructions", "화장품 입니다."));

        return Product.of(
                seller,
                productName,
                description,
                Status.ACTIVE
        );
    }

    private ProductNotice createProductNotice(Map<String, Object> data, Product product) {
        return ProductNotice.of(
                product,
                (String) data.getOrDefault("capacity", "용량 정보 없음"),
                (String) data.getOrDefault("specifications", "모든 피부용"),
                "제조일로부터 3년",
                (String) data.getOrDefault("usage_instructions", "사용법을 확인해주세요."),
                (String) data.getOrDefault("manufacturer", "제조사 정보 없음"),
                (String) data.getOrDefault("manufacturer", "제조사"),
                (String) data.getOrDefault("country_of_origin", "원산지 정보 없음"),
                false,
                cleanText((String) data.getOrDefault("precautions", "사용 전 패치테스트를 권장합니다.")),
                (String) data.getOrDefault("warranty_policy", "공정거래위원회 고시에 따라 보상받을 수 있습니다."),
                (String) data.getOrDefault("customer_service_number", "고객센터: 1588-0000")
        );
    }

    private ProductOption createProductOption(Map<String, Object> data, Product product, ProductOptionInfo optionInfo) {
        Integer price = parsePrice(data.get("price"));
        String imageUrl = (String) data.get("img_url");

        return ProductOption.of(
                product,
                optionInfo.getOptionName(),
                price,
                imageUrl,
                optionInfo.getIngredients()
        );
    }

    private void linkProductCategory(Map<String, Object> data, Product product) {
        String category1 = (String) data.get("category_1");
        String category2 = (String) data.get("category_2");

        log.debug("🔍 Processing product: '{}' with categories: '{}' - '{}'",
                product.getName(), category1, category2);

        String categoryKey = category1 + "-" + category2;
        Long categoryId = CATEGORY_MAPPING.getOrDefault(categoryKey, CATEGORY_MAPPING.get("DEFAULT"));

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            try {
                ProductCategory productCategory = ProductCategory.of(product, category);
                ProductCategory savedProductCategory = productCategoryRepository.save(productCategory);

                log.debug("✅ Successfully linked product '{}' (ID: {}) to category '{}' (ID: {}), saved with ID: {}",
                        product.getName(), product.getId(),
                        category.getName(), category.getId(),
                        savedProductCategory.getId());

                boolean exists = productCategoryRepository.existsByProductIdAndCategoryId(
                        product.getId(), category.getId());
                log.debug("🔍 Verification - exists in DB: {}", exists);
            } catch (Exception e) {
                log.error("❌ Failed to link product '{}' to category '{}': {}",
                        product.getName(), category.getName(), e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ Category not found for ID: {}, using default", categoryId);
        }
    }

    private Seller getDefaultSeller() {
        return sellerRepository.findById(1L).orElse(null);
    }

    private Integer parsePrice(Object priceObj) {
        if (priceObj == null) return 0;

        if (priceObj instanceof Integer) {
            return (Integer) priceObj;
        }

        if (priceObj instanceof String) {
            try {
                return Integer.parseInt(((String) priceObj).replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                log.warn("⚠️ Failed to parse price: {}", priceObj);
                return 0;
            }
        }

        return 0;
    }

    private String cleanProductName(String productName) {
        if (productName == null) return "상품명 없음";

        return productName.replaceAll("\\[.*?\\]", "").trim();
    }

    private String cleanText(String text) {
        if (text == null) return "";

        return text.replaceAll("\\s+", " ").trim();
    }

    private static class ProductOptionInfo {
        private final String optionName;
        private final String ingredients;

        public ProductOptionInfo(String optionName, String ingredients) {
            this.optionName = optionName;
            this.ingredients = ingredients;
        }

        public String getOptionName() {
            return optionName;
        }

        public String getIngredients() {
            return ingredients;
        }
    }
}
