package com.ururulab.ururu.global.config.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ururulab.ururu.product.domain.entity.*;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.*;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ProductSampleDataLoader implements CommandLineRunner{

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final ObjectMapper objectMapper;

    private static final Map<String, Long> CATEGORY_MAPPING = new HashMap<>();

    static {
        // 메이크업
        CATEGORY_MAPPING.put("메이크업-립", 56L);
        CATEGORY_MAPPING.put("메이크업-베이스", 63L);
        CATEGORY_MAPPING.put("메이크업-아이", 74L);

        // 스킨케어
        CATEGORY_MAPPING.put("스킨케어-스킨", 2L);
        CATEGORY_MAPPING.put("스킨케어-에센스", 3L);
        CATEGORY_MAPPING.put("스킨케어-크림", 4L);
        CATEGORY_MAPPING.put("스킨케어-로션", 7L);

        // 바디케어
        CATEGORY_MAPPING.put("바디-데오드란트", 86L);
        CATEGORY_MAPPING.put("바디-로션", 86L);

        // 선케어
        CATEGORY_MAPPING.put("선케어-선크림", 44L);

        // 기본값
        CATEGORY_MAPPING.put("DEFAULT", 1L);
    }

    @Override
    public void run(String... args) throws Exception {
        // 개발 환경에서만 실행 (운영 환경 방지)
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
                "/data/skincareSkin.json"
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
                        inputStream, new TypeReference<List<Map<String, Object>>>() {
                        }
                );

                log.debug("Found {} proudcts in sample data file", productDataList.size());

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
                            log.debug("Progress: {}/{} products processd", i + 1, productDataList.size());
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

    private int processProductData(Map<String, Object>data, Seller seller) {
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

        linkProductCategory(data, savedProduct);
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

                // 성분 정리
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
        String proudctName = cleanProductName((String) data.get("prd_name"));
        String brand = (String) data.get("brand");

        String description = (String) data.getOrDefault("specifications",
                (String) data.getOrDefault("usage_instructions", "화장품 입니다."));

        return Product.of(
                seller,
                proudctName,
                description,
                Status.ACTIVE
        );
    }

    private ProductNotice createProductNotice(Map<String, Object> data, Product product){
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

        String categoryKey = category1 + "-" + category2;
        Long categoryId = CATEGORY_MAPPING.getOrDefault(categoryKey, CATEGORY_MAPPING.get("DEFAULT"));

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null) {
            ProductCategory productCategory = ProductCategory.of(product, category);
            productCategoryRepository.save(productCategory);

            log.debug("🔗 Linked product '{}' to category '{}'",
                    product.getName(), category.getName());
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

        // [브랜드픽], [한정판] 등의 프로모션 텍스트 제거
        return productName.replaceAll("\\[.*?\\]", "").trim();
    }

    private String cleanText(String text) {
        if (text == null) return "";

        // 불필요한 공백 및 개행 정리
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
