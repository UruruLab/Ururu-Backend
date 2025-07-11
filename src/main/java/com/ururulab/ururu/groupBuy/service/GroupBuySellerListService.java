package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuySellerListResponse;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuySellerListService {

    private final GroupBuyRepository groupBuyRepository;
    private final SellerRepository sellerRepository;

    public Page<GroupBuySellerListResponse> getSellerGroupBuyList(Long sellerId, Pageable pageable) {
        // 본인 인증 확인
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(SELLER_NOT_FOUND));

        Page<GroupBuy> groupBuyPage = groupBuyRepository.findBySellerIdWithPagination(sellerId, pageable);

        if (groupBuyPage.isEmpty()) {
            throw new BusinessException(GROUPBUY_EMPTY);
        }

        // 2. 해당 페이지의 GroupBuy들과 Options를 JOIN FETCH로 조회
        List<Long> groupBuyIds = groupBuyPage.getContent().stream()
                .map(GroupBuy::getId)
                .toList();

        List<GroupBuy> groupBuysWithOptions = groupBuyRepository.findByIdsWithOptions(groupBuyIds);

        List<GroupBuySellerListResponse> responses = groupBuysWithOptions.stream()
                .map(groupBuy -> {
                    List<GroupBuyOption> options = groupBuy.getOptions();
                    Integer orderCount = options.stream()
                            .mapToInt(GroupBuyOption::getSoldQuantity)
                            .sum();
                    return GroupBuySellerListResponse.from(groupBuy, options, orderCount);
                })
                .toList();

        return new PageImpl<>(responses, pageable, groupBuyPage.getTotalElements());
    }
}
