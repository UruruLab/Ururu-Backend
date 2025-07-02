package com.ururulab.ururu.groupBuy.listener;

import com.ururulab.ururu.groupBuy.event.GroupBuyDetailImageUploadEvent;
import com.ururulab.ururu.groupBuy.service.GroupBuyDetailImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuyDetailImageUploadListener {
    private final GroupBuyDetailImageService groupBuyDetailImageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("imageUploadExecutor")
    public void handleGroupBuyDetailImageUpload(GroupBuyDetailImageUploadEvent event) {
        log.info("Starting async detail image upload for groupBuy: {}, {} images",
                event.groupBuyId(), event.images().size());

        groupBuyDetailImageService.uploadDetailImagesAsync(
                event.groupBuyId(),
                event.images()
        );

        log.info("Completed async detail image upload for groupBuy: {}", event.groupBuyId());
    }
}
