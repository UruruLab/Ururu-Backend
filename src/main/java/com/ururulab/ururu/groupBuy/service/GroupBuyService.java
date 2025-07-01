package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.controller.dto.response.GroupBuyCreateResponse;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupBuyService {

    private final GroupBuyRepository groupBuyRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final GroupBuyValidator groupBuyValidator;

    @Transactional
    public GroupBuyCreateResponse createGroupBuy(GroupBuyRequest request, Long sellerId) {
        log.info("Creating group buy for seller: {}, productId: {}", sellerId, request.productId());

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(SELLER_NOT_FOUND));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));

        groupBuyValidator.validateCritical(request);

        GroupBuy savedGroupBuy = groupBuyRepository.save(request.toEntity(product, seller, null));

        log.info("Group buy created successfully with ID: {} for seller: {}",
                savedGroupBuy.getId(), sellerId);

        return GroupBuyCreateResponse.from(savedGroupBuy);
    }
}
