package com.ururulab.ururu.groupBuy.listener;

import com.ururulab.ururu.groupBuy.event.GroupBuyThumbnailDeleteEvent;
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
public class GroupBuyThumbnailDeleteListener {

    private final ImageService imageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("imageDeleteExecutor")
    public void handleGroupBuyThumbnailDelete(GroupBuyThumbnailDeleteEvent event) {
        log.info("Starting async thumbnail deletion for groupBuy: {}, {} images",
                event.groupBuyId(), event.imageUrls().size());

        for (String imageUrl : event.imageUrls()) {
            try {
                imageService.deleteImage(imageUrl);
                log.info("Successfully deleted thumbnail: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to delete thumbnail: {}", imageUrl, e);
                // 실패해도 다른 이미지들은 계속 처리
            }
        }

        log.info("Completed async thumbnail deletion for groupBuy: {}", event.groupBuyId());
    }
}
