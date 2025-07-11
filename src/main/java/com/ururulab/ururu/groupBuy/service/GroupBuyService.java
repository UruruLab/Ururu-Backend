package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyRequest;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyCreateResponse;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupBuyService {

    private final GroupBuyRepository groupBuyRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final GroupBuyValidator groupBuyValidator;
    private final GroupBuyOptionService groupBuyOptionService;
    private final GroupBuyThumbnailService groupBuyThumbnailService;
    private final GroupBuyDetailImageService groupBuyDetailImageService;
    private final GroupBuyPriceService groupBuyPriceService;

    @Transactional
    public GroupBuyCreateResponse createGroupBuy(GroupBuyRequest request, Long sellerId, MultipartFile thumbnail,
                                                 List<MultipartFile> detailImages) {
        log.info("Creating group buy for seller: {}, productId: {}", sellerId, request.productId());

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(SELLER_NOT_FOUND));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(PRODUCT_NOT_FOUND));

        groupBuyValidator.validateCritical(request, sellerId);

        if (product.getStatus() == Status.INACTIVE) {
            product.updateStatus(Status.ACTIVE);
            log.info("Product {} activated for GroupBuy", product.getId());
        }

        GroupBuy savedGroupBuy = groupBuyRepository.save(request.toEntity(product, seller, null));
        groupBuyOptionService.createGroupBuyOptions(savedGroupBuy, request.options());
        groupBuyPriceService.updateDisplayFinalPrice(savedGroupBuy.getId());

        // 2. 이미지 업로드 (비동기)
        if (thumbnail != null) {
            groupBuyThumbnailService.uploadThumbnail(savedGroupBuy.getId(), thumbnail);
        }
        if (detailImages != null && !detailImages.isEmpty()) {
            groupBuyDetailImageService.uploadDetailImages(savedGroupBuy.getId(), detailImages);
        }

        log.info("Group buy created successfully with ID: {} for seller: {}",
                savedGroupBuy.getId(), sellerId);

        return GroupBuyCreateResponse.from(savedGroupBuy);
    }
}
