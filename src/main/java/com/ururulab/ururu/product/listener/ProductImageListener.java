package com.ururulab.ururu.product.listener;

import com.ururulab.ururu.product.event.ProductImageUploadEvent;
import com.ururulab.ururu.product.service.ProductOptionImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductImageListener {

    private final ProductOptionImageService productOptionImageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("imageUploadExecutor")
    public void handleProductImageUpload(ProductImageUploadEvent event) {
        log.info("Starting async image upload for product: {}", event.getProductId());

        productOptionImageService.uploadImagesAsync(
                event.getProductId(),
                event.getImages()
        );

        log.info("Completed async image upload for product: {}", event.getProductId());
    }
}
