package com.ururulab.ururu.product.controller;

import com.ururulab.ururu.product.domain.dto.request.ProductRequest;
import com.ururulab.ururu.product.domain.dto.response.ProductResponse;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {

        //Long sellerId = 1L; // TODO: 실제 인증 시스템과 연동
        //ProductResponse productResponse = productService.createProduct(productRequest, sellerId);

        ProductResponse productResponse = productService.createProduct(productRequest);
        log.info("Product created successfully with ID: {}", productResponse.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(productResponse);
    }
}
