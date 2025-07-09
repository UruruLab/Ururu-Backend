package com.ururulab.ururu.global.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.error.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 전역 예외 처리기.
 *
 * <p>애플리케이션 전반에서 발생하는 예외를 일관된 형태로 처리하고 클라이언트에게 적절한 응답을 제공합니다.</p>
 */
@Slf4j
@RestControllerAdvice
public final class GlobalExceptionHandler {

	/**
	 * BusinessException 처리 - 모든 비즈니스 로직 예외의 중앙 처리.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleBusinessException(final BusinessException ex) {
		final ErrorCode errorCode = ex.getErrorCode();
		log.warn("Business exception [{}]: {}", errorCode.getCode(), ex.getMessage());
		
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ApiResponseFormat.fail(errorCode));
	}

	/**
	 * JWT 라이브러리 예외 처리 - ErrorCode 기반으로 통합.
	 */
	@ExceptionHandler({
			io.jsonwebtoken.JwtException.class,
			ExpiredJwtException.class,
			MalformedJwtException.class,
			SecurityException.class
	})
	public ResponseEntity<ApiResponseFormat<Void>> handleJwtException(
			final RuntimeException exception) {
		
		final ErrorCode errorCode = determineJwtErrorCode(exception);
		log.warn("JWT processing failed [{}]: {}", errorCode.getCode(), exception.getMessage());
		
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ApiResponseFormat.fail(errorCode));
	}

	/**
	 * 요청 데이터 검증 실패 예외 처리.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleValidation(
			final MethodArgumentNotValidException exception
	) {
		final String errorMessage = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.findFirst()
				.map(FieldError::getDefaultMessage)
				.orElse("요청 데이터가 유효하지 않습니다.");

		log.warn("Validation failed: {}", errorMessage);
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponseFormat.fail("VALIDATION_ERROR", errorMessage));
	}

	/**
	 * DB 무결성 제약조건 위반 예외 처리.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleDataIntegrityViolation(
			final DataIntegrityViolationException exception
	) {
		log.warn("Data integrity violation: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(ApiResponseFormat.fail("DATA_INTEGRITY_VIOLATION", "이미 사용 중인 정보입니다."));
	}

	/**
	 * 이미지 크기 초과 예외 처리.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleMaxUploadSizeExceeded(
			final MaxUploadSizeExceededException exception
	) {
		log.warn("File size exceeded: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(ApiResponseFormat.fail("IMAGE_SIZE_EXCEEDED", "파일 크기가 제한을 초과했습니다."));
	}
	
	/**
	 * IllegalArgumentException 처리.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleIllegalArgument(
			final IllegalArgumentException exception
	) {
		log.warn("Invalid argument: {}", exception.getMessage());
		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponseFormat.fail("INVALID_ARGUMENT", exception.getMessage()));
	}

	/**
	 * 예상하지 못한 모든 예외 처리.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleGeneral(final Exception exception) {
		log.error("Unexpected error occurred: {}", exception.getMessage(), exception);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponseFormat.fail("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
	}

	/**
	 * JWT 예외 타입에 따른 ErrorCode 결정.
	 */
	private ErrorCode determineJwtErrorCode(final RuntimeException exception) {
		if (exception instanceof ExpiredJwtException) {
			return ErrorCode.EXPIRED_JWT_TOKEN;
		}
		if (exception instanceof MalformedJwtException) {
			return ErrorCode.MALFORMED_JWT_TOKEN;
		}
		return ErrorCode.INVALID_JWT_TOKEN;
	}
	/**
	 * AI 서비스 연결 타임아웃 예외 처리.
	 */
	@ExceptionHandler(java.net.SocketTimeoutException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleSocketTimeoutException(final java.net.SocketTimeoutException e) {
		log.error("AI 서비스 타임아웃 발생: {}", e.getMessage(), e);
		return ResponseEntity
				.status(HttpStatus.REQUEST_TIMEOUT)
				.body(ApiResponseFormat.fail(ErrorCode.AI_SERVICE_TIMEOUT));
	}

	/**
	 * Spring RestClient 예외 처리 (AI 서비스 통신 관련).
	 */
	@ExceptionHandler(org.springframework.web.client.RestClientException.class)
	public ResponseEntity<ApiResponseFormat<Void>> handleRestClientException(final org.springframework.web.client.RestClientException e) {
		log.error("AI 서비스 통신 오류: {}", e.getMessage(), e);

		final String errorMessage = e.getMessage();
		if (errorMessage != null) {
			if (errorMessage.contains("timeout")) {
				return ResponseEntity
						.status(HttpStatus.REQUEST_TIMEOUT)
						.body(ApiResponseFormat.fail(ErrorCode.AI_SERVICE_TIMEOUT));
			}

			if (errorMessage.contains("Connection") || errorMessage.contains("refused")) {
				return ResponseEntity
						.status(HttpStatus.BAD_GATEWAY)
						.body(ApiResponseFormat.fail(ErrorCode.AI_SERVICE_CONNECTION_FAILED));
			}
		}

		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ApiResponseFormat.fail(ErrorCode.AI_SERVICE_UNAVAILABLE));
	}
}
