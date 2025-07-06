package com.ururulab.ururu.groupBuy.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyOptionRepository;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyDetailResponse;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import com.ururulab.ururu.order.domain.repository.OrderItemRepository;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyDetailService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionRepository groupBuyOptionRepository;
    private final GroupBuyStockService stockService;
    private final OrderItemRepository orderItemRepository;
    private final SellerRepository sellerRepository;
    private final GroupBuyValidator groupBuyValidator;

    /**
     * 공동구매 상세 정보 조회
     *
     * @param sellerId 판매자 ID
     * @param groupBuyId 공동구매 ID
     * @return 공동구매 상세 정보 (남은 시간 계산 포함)
     * @throws BusinessException 공동구매를 찾을 수 없는 경우
     */
    public GroupBuyDetailResponse getSellerGroupBuyDetail(Long sellerId, Long groupBuyId) {
        log.info("Fetching group buy detail for ID: {}", groupBuyId);

        // 판매자 존재 여부 검증
        groupBuyValidator.validateSellerExists(sellerId);

        // 공동구매 기본 정보 및 연관 데이터 조회 (한 번의 쿼리로 최적화)
        GroupBuy groupBuy = findGroupBuyWithAllAssociations(groupBuyId);

        // 판매자가 등록한 공동구매인지 검증
        groupBuyValidator.validateSellerAccess(sellerId, groupBuy);

        // 옵션 정보 조회 (별도 쿼리 - ProductOption 정보 포함)
        List<GroupBuyOption> options = findGroupBuyOptionsWithProductOptions(groupBuy);

        // 이미지 정보는 이미 페치된 데이터에서 추출
        List<GroupBuyImage> images = extractAndSortImages(groupBuy);

        // 현재 재고 조회
        Map<Long, Integer> currentStocks = stockService.getCurrentStocksByGroupBuy(groupBuyId, options);

        // 실시간 주문 수 조회
        int currentOrderCount = orderItemRepository.getTotalQuantityByGroupBuyId(groupBuyId);

        log.info("Successfully fetched group buy detail - ID: {}, options: {}, images: {}",
                groupBuyId, options.size(), images.size());

        return GroupBuyDetailResponse.from(groupBuy, options, images, currentStocks, currentOrderCount);
    }

    /**
     * 공동구매와 모든 필요한 연관 엔티티를 한 번에 조회
     * - Product, ProductCategory, Category, ProductOption
     * - GroupBuyImages
     */
    private GroupBuy findGroupBuyWithAllAssociations(Long groupBuyId) {
        return groupBuyRepository.findByIdWithDetails(groupBuyId)
                .orElseThrow(() -> {
                    log.error("Group buy not found with ID: {}", groupBuyId);
                    return new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId);
                });
    }


    /**
     * 공동구매 옵션과 연관된 ProductOption 정보를 함께 조회
     * 가격, 이미지 등의 상세 정보가 필요하므로 별도 조회
     */
    private List<GroupBuyOption> findGroupBuyOptionsWithProductOptions(GroupBuy groupBuy) {
        return groupBuyOptionRepository.findAllByGroupBuy(groupBuy);
    }

    /**
     * 이미 페치된 GroupBuy에서 이미지 정보를 필터링하고 정렬
     * - 삭제되지 않은 이미지만 필터링
     * - displayOrder 순으로 정렬
     */
    private List<GroupBuyImage> extractAndSortImages(GroupBuy groupBuy) {
        return groupBuy.getGroupBuyImages().stream()
                .filter(image -> !image.getIsDeleted()) // 삭제되지 않은 이미지만
                .sorted((img1, img2) -> Integer.compare(img1.getDisplayOrder(), img2.getDisplayOrder()))
                .toList();
    }

    // Service
    public GroupBuyDetailResponse getPublicGroupBuyDetail(Long groupBuyId) {
        // 1번 쿼리: 메인 데이터 (DRAFT 제외)
        GroupBuy groupBuy = groupBuyRepository.findPublicGroupBuyWithDetails(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        // 2번 쿼리: 이미지
        List<GroupBuyImage> images = groupBuyRepository.findByIdWithImages(groupBuyId)
                .map(this::extractAndSortImages)
                .orElse(List.of());

        // 3번 쿼리: 옵션
        List<GroupBuyOption> options = groupBuyOptionRepository.findAllByGroupBuy(groupBuy);

        Map<Long, Integer> currentStocks = stockService.getCurrentStocksByGroupBuy(groupBuyId, options);

        int currentOrderCount = orderItemRepository.getTotalQuantityByGroupBuyId(groupBuyId);

        return GroupBuyDetailResponse.from(groupBuy, options, images, currentStocks, currentOrderCount);
    }

    /**
     * 공동구매 기본 정보만 조회 (연관 데이터 없이)
     * 간단한 정보만 필요한 경우 사용
     *
     * @param groupBuyId 공동구매 ID
     * @return 공동구매 기본 정보
     */
    public GroupBuy getGroupBuyBasicInfo(Long groupBuyId) {
        log.debug("Fetching basic group buy info for ID: {}", groupBuyId);
        return groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));
    }
}
