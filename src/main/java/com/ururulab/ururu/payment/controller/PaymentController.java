package com.ururulab.ururu.payment.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.payment.controller.dto.request.PaymentConfirmRequestDto;
import com.ururulab.ururu.payment.controller.dto.request.PaymentRequestDto;
import com.ururulab.ururu.payment.controller.dto.response.PaymentConfirmResponseDto;
import com.ururulab.ururu.payment.controller.dto.response.PaymentFailResponseDto;
import com.ururulab.ururu.payment.controller.dto.response.PaymentResponseDto;
import com.ururulab.ururu.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "결제 관련 API")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청 생성", description = "공동구매 주문서에 대한 결제 요청을 생성하고 토스페이먼츠 SDK 실행용 정보를 반환합니다. 포인트 사용 시 즉시 차감됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "결제 요청 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문"),
            @ApiResponse(responseCode = "409", description = "이미 진행 중인 결제 또는 주문 상태 오류"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/request")
    public ResponseEntity<ApiResponseFormat<PaymentResponseDto>> createPaymentRequest(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PaymentRequestDto request
    ) {
        log.debug("결제 요청 생성 - 회원ID: {}, 주문ID: {}", memberId, request.orderId());

        PaymentResponseDto response = paymentService.createPaymentRequest(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("결제 요청이 생성되었습니다", response));
    }

    @Operation(summary = "토스 결제 성공 리다이렉트", description = "토스페이먼츠에서 결제 성공 시 호출되는 리다이렉트 엔드포인트입니다. 실제 결제 승인은 별도 confirm API에서 처리됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 성공 처리 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터")
    })
    @GetMapping("/success")
    public ResponseEntity<ApiResponseFormat<Void>> handlePaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Integer amount
    ) {
        log.debug("결제 성공 리다이렉트 - paymentKey: {}, orderId: {}", paymentKey, orderId);

        paymentService.handlePaymentSuccess(paymentKey, orderId, amount);

        return ResponseEntity.ok(
                ApiResponseFormat.success("결제가 완료되었습니다")
        );
    }

    @Operation(summary = "토스 결제 실패 리다이렉트", description = "토스페이먼츠에서 결제 실패 시 호출되는 리다이렉트 엔드포인트입니다. 주문을 취소하고 사용된 포인트를 복구합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 실패 처리 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 파라미터"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문")
    })
    @GetMapping("/fail")
    public ResponseEntity<ApiResponseFormat<PaymentFailResponseDto>> handlePaymentFail(
            @RequestParam String orderId,
            @RequestParam String code,
            @RequestParam String message
    ) {
        log.debug("결제 실패 리다이렉트 - orderId: {}, code: {}", orderId, code);

        PaymentFailResponseDto response = paymentService.handlePaymentFail(orderId, code, message);

        return ResponseEntity.ok(
                ApiResponseFormat.success("결제 실패 처리가 완료되었습니다", response)
        );
    }

    @Operation(summary = "결제 승인", description = "토스페이먼츠 API를 호출하여 실제 결제를 승인합니다. 승인 성공 시 주문 상태가 ORDERED로 변경되고 재고가 확정됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 결제 승인 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 결제"),
            @ApiResponse(responseCode = "409", description = "결제 가능 상태가 아님"),
            @ApiResponse(responseCode = "500", description = "토스 API 호출 실패 또는 서버 오류")
    })
    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<ApiResponseFormat<PaymentConfirmResponseDto>> confirmPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentConfirmRequestDto request
    ) {
        log.debug("결제 승인 요청 - paymentId: {}, paymentKey: {}", paymentId, request.paymentKey());

        PaymentConfirmResponseDto response = paymentService.confirmPayment(paymentId, request);

        return ResponseEntity.ok(
                ApiResponseFormat.success("결제 승인이 완료되었습니다", response)
        );
    }

    @Operation(summary = "토스 웹훅 수신", description = "토스페이먼츠에서 결제 상태 변경을 비동기로 알려주는 웹훅을 처리합니다. 결제 상태 동기화 및 누락된 처리를 보정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "웹훅 처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 웹훅 데이터")
    })
    @PostMapping("/webhooks/toss")
    public ResponseEntity<ApiResponseFormat<Void>> handleTossWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Toss-Signature", required = false) String signature
    ) {

        paymentService.handleTossWebhookWithValidation(request, signature);
        return ResponseEntity.ok(ApiResponseFormat.success("웹훅 처리 완료"));
    }

    @Operation(summary = "결제 상태 조회", description = "paymentKey로 결제 진행 상태를 조회합니다. confirm 실패 시 프론트에서 폴링용으로 사용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (다른 사용자의 결제)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 결제")
    })
    @GetMapping("/{paymentKey}/status")
    public ResponseEntity<ApiResponseFormat<PaymentConfirmResponseDto>> getPaymentStatus(
            @PathVariable String paymentKey,
            @AuthenticationPrincipal Long memberId
    ) {
        log.debug("결제 상태 조회 - paymentKey: {}, memberId: {}", paymentKey, memberId);

        PaymentConfirmResponseDto response = paymentService.getPaymentStatusByKey(paymentKey, memberId);

        return ResponseEntity.ok(
                ApiResponseFormat.success("결제 상태 조회 성공", response)
        );
    }

}