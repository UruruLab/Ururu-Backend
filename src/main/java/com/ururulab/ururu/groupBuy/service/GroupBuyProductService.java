package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyCreatePageResponse;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.PRODUCT_NOT_EXIST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyProductService {
    private final ProductRepository productRepository;

    public GroupBuyCreatePageResponse getGroupBuyCreateData(Long sellerId) {

        // 해당 판매자의 활성화된 상품과 옵션을 함께 조회
        List<Product> products = productRepository.findBySellerIdAndStatusWithOptions(sellerId, Status.INACTIVE);

        if (products == null || products.isEmpty()) {
            throw new BusinessException(PRODUCT_NOT_EXIST);
        }

        return GroupBuyCreatePageResponse.from(products);

    }
}
