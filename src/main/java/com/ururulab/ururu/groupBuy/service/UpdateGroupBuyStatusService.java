package com.ururulab.ururu.groupBuy.service;


import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.domain.repository.GroupBuyRepository;
import com.ururulab.ururu.groupBuy.dto.request.GroupBuyStatusUpdateRequest;
import com.ururulab.ururu.groupBuy.service.validation.GroupBuyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ururulab.ururu.global.exception.error.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateGroupBuyStatusService {

    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionService groupBuyOptionService;
    private final GroupBuyValidator groupBuyValidator;

    /**
     * 판매자용 공동구매 상태 업데이트 (DRAFT → OPEN)
     *
     * @param sellerId 판매자 ID
     * @param groupBuyId 공동구매 ID
     * @param request 상태 업데이트 요청
     * @throws BusinessException 권한이 없거나 잘못된 상태 변경인 경우
     */
    @Transactional
    public void updateGroupBuyStatus(Long sellerId, Long groupBuyId, GroupBuyStatusUpdateRequest request) {
        log.info("Updating group buy status - sellerId: {}, groupBuyId: {}, newStatus: {}",
                sellerId, groupBuyId, request.status());

        // 1. 요청 검증
        groupBuyValidator.validateStatusUpdateRequest(request);

        // 2. 공동구매 조회 및 판매자 권한 검증
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NOT_FOUND, groupBuyId));

        groupBuyValidator.validateSellerAccess(sellerId, groupBuy);

        // 3. 상태 변경 검증
        groupBuyValidator.validateStatusChange(groupBuy.getStatus(), request.status());

        // 4. 공동구매 오픈 조건 검증
        groupBuyValidator.validateGroupBuyOpenConditions(groupBuy);

        // 5. 상태 업데이트
        groupBuy.startGroupBuy(request.status());

        // 6. 옵션 할인가 계산 (OPEN 시점에 초기 할인율 적용)
        if (request.status() == GroupBuyStatus.OPEN) {
            groupBuyOptionService.calculateAndSetSalePrices(groupBuy);
        }

        groupBuyRepository.save(groupBuy);

        log.info("Group buy status updated successfully - groupBuyId: {}, oldStatus: {}, newStatus: {}",
                groupBuyId, groupBuy.getStatus(), request.status());
    }
}
