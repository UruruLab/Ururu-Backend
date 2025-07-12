package com.ururulab.ururu.groupBuy.listener;

import com.ururulab.ururu.groupBuy.event.GroupBuyThumbnailUploadEvent;
import com.ururulab.ururu.groupBuy.service.GroupBuyThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupBuyThumbnailUploadListener {
    private final GroupBuyThumbnailService groupBuyThumbnailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("imageUploadExecutor")
    public void handleGroupBuyThumbnailUpload(GroupBuyThumbnailUploadEvent event) {
        log.info("Starting async thumbnail upload for groupBuy: {}", event.groupBuyId());

        groupBuyThumbnailService.uploadThumbnailAsync(
                event.groupBuyId(),
                event.originalFilename(),
                event.tempFilePath(),
                event.thumbnailHash()
        );

        log.info("Completed async thumbnail upload for groupBuy: {}", event.groupBuyId());
    }
}
