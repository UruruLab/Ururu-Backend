package com.ururulab.ururu.groupBuy.listener;

import com.ururulab.ururu.groupBuy.event.GroupBuyDetailImageDeleteEvent;
import com.ururulab.ururu.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupBuyDetailImageDeleteListener {

    private final ImageService imageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("imageDeleteExecutor")
    public void handleGroupBuyDetailImageDelete(GroupBuyDetailImageDeleteEvent event) {
        log.info("Starting async detail image deletion for groupBuy: {}, {} images",
                event.groupBuyId(), event.imageUrls().size());

        for (String imageUrl : event.imageUrls()) {
            try {
                imageService.deleteImage(imageUrl);
                log.info("Successfully deleted detail image: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to delete detail image: {}", imageUrl, e);
                // 실패해도 다른 이미지들은 계속 처리
            }
        }

        log.info("Completed async detail image deletion for groupBuy: {}", event.groupBuyId());
    }
}
