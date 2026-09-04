package com.ururulab.ururu.payment.infrastructure;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.payment.dto.response.TossPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private final RestClient restClient;

    @Value("${toss.payments.secret-key}")
    private String secretKey;

    @Value("${toss.payments.base-url}")
    private String baseUrl;

    public TossPaymentResponseDto confirm(String paymentKey, String orderId, int amount, String idempotencyKey) {
        return post("/v1/payments/confirm", Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        ), idempotencyKey);
    }

    public TossPaymentResponseDto getPayment(String paymentKey) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/v1/payments/{paymentKey}", paymentKey)
                    .header("Authorization", authorization())
                    .retrieve()
                    .body(TossPaymentResponseDto.class);
        } catch (Exception e) {
            log.error("토스 결제 조회 실패", e);
            throw new BusinessException(ErrorCode.TOSS_API_CALL_FAILED);
        }
    }

    public void cancel(String paymentKey, String reason, int amount, String idempotencyKey) {
        post("/v1/payments/" + paymentKey + "/cancel", Map.of(
                "cancelReason", reason,
                "cancelAmount", amount
        ), idempotencyKey);
    }

    private TossPaymentResponseDto post(String path, Map<String, Object> body, String idempotencyKey) {
        try {
            return restClient.post()
                    .uri(baseUrl + path)
                    .header("Authorization", authorization())
                    .header("Idempotency-Key", idempotencyKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(TossPaymentResponseDto.class);
        } catch (Exception e) {
            log.error("토스 결제 API 호출 실패 - path: {}", path, e);
            throw new BusinessException(ErrorCode.TOSS_API_CALL_FAILED);
        }
    }

    private String authorization() {
        String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
