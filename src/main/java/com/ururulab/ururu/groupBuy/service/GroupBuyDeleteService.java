package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.event.GroupBuyDetailImageDeleteEvent;
import com.ururulab.ururu.groupBuy.event.GroupBuyThumbnailDeleteEvent;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyDeleteService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteGroupBuy(Long groupBuyId, Long sellerId) {
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND));

        if (!groupBuy.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(GROUPBUY_SELLER_ACCESS_DENIED);
        }

        if (groupBuy.getStatus() != GroupBuyStatus.DRAFT) {
            throw new BusinessException(GROUPBUY_DELETE_NOT_ALLOWED);
        }

        // 연관된 상품을 INACTIVE로 변경
        Product product = groupBuy.getProduct();
        if (product.getStatus() == Status.ACTIVE) {
            product.updateStatus(Status.INACTIVE);
            log.info("Product {} deactivated after GroupBuy deletion", product.getId());
        }

        // 1. 썸네일 이미지 삭제 이벤트 발행
        if (groupBuy.getThumbnailUrl() != null) {
            eventPublisher.publishEvent(new GroupBuyThumbnailDeleteEvent(
                    groupBuy.getId(),
                    List.of(groupBuy.getThumbnailUrl())
            ));
        }

        // 2. 상세 페이지 이미지 삭제 이벤트 발행
        List<String> detailImageUrls = groupBuy.getGroupBuyImages().stream()
                .map(image -> image.getImageUrl())
                .filter(Objects::nonNull)
                .toList();

        if (!detailImageUrls.isEmpty()) {
            eventPublisher.publishEvent(new GroupBuyDetailImageDeleteEvent(
                    groupBuy.getId(),
                    detailImageUrls
            ));
        }

        // 하위 옵션 먼저 삭제
        groupBuyOptionRepository.deleteAllByGroupBuyId(groupBuyId);

        groupBuyRepository.delete(groupBuy);
    }
}
