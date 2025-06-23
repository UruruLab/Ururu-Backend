package com.ururulab.ururu.product.service;

import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.ProductNoticeResponse;
import com.ururulab.ururu.product.domain.dto.response.ProductResponse;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductNotice;
import com.ururulab.ururu.product.domain.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductNoticeRepository productNoticeRepository;
    private final ProductOptionRepository productOptionRepository;

    /**
     * Product 상품을 저장합니다
     *
     * @param productRequest
     * @return
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        Product savedProduct = productRepository.save(productRequest.toEntity());

        // 상품 정보고시 저장
        ProductNoticeResponse productNoticeResponse = saveProductNotice(savedProduct, productRequest);

        return ProductResponse.from(savedProduct, categoryResponses, productOptionResponses, productNoticeResponse);
    }

    /**
     * 상품 정보고시를 저장합니다.
     *
     * @param product 저장된 상품 엔티티
     * @param productRequest 상품 등록 요청 정보
     * @return 저장된 상품 정보고시 응답
     */
    private ProductNoticeResponse saveProductNotice(Product product, ProductRequest productRequest) {
        ProductNotice productNotice = productRequest.productNotice().toEntity(product);
        ProductNotice savedProductNotice = productNoticeRepository.save(productNotice);

        return ProductNoticeResponse.from(savedProductNotice);
    }
}
