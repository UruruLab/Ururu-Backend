package com.ururulab.ururu.product.listner;

import com.ururulab.ururu.image.service.ImageService;
import com.ururulab.ururu.product.event.ProductImageDeleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProductImageDeleteListener {

    private final ImageService imageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("imageDeleteExecutor")
    public void handleProductImageDelete(ProductImageDeleteEvent event) {
        log.info("Starting async image deletion for product: {}, {} images",
                event.productId(), event.imageUrls().size());

        for (String imageUrl : event.imageUrls()) {
            try {
                imageService.deleteImage(imageUrl);
                log.info("Successfully deleted image: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to delete image: {}", imageUrl, e);
                // 실패해도 다른 이미지들은 계속 처리
            }
        }

        log.info("Completed async image deletion for product: {}", event.productId());
    }
}
