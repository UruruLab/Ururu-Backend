package com.ururulab.ururu.global.exception.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
	// --- 공통 ---
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON001", "잘못된 HTTP 메서드를 호출했습니다."),
	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "COMMON002", "요청 파라미터가 유효하지 않습니다."),

	// --- 리뷰 ---
	REVIEW_NOT_FOUND(HttpStatus.BAD_REQUEST, "REVIEW001", "리뷰가 존재하지 않습니다."),
	REVIEW_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "REVIEW002", "이미지는 최대 %d개까지 첨부할 수 있습니다."),

	// --- 이미지 ---
	INVALID_IMAGE_FILENAME(HttpStatus.BAD_REQUEST, "IMAGE001", "파일명이 없거나 확장자를 찾을 수 없습니다."),
	INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "IMAGE002", "지원하지 않는 확장자입니다."),
	INVALID_IMAGE_MIME(HttpStatus.BAD_REQUEST, "IMAGE003", "지원하지 않는 MIME 타입입니다."),
	IMAGE_FORMAT_MISMATCH(HttpStatus.BAD_REQUEST, "IMAGE004", "확장자와 MIME 타입이 일치하지 않습니다: file=%s"),
	IMAGE_FILENAME_MISSING(HttpStatus.BAD_REQUEST, "IMAGE005", "파일명이 없습니다."),
	IMAGE_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE006", "이미지 변환에 실패했습니다."),
	IMAGE_PROCESSING_FAILED(HttpStatus.BAD_REQUEST, "IMAGE007", "이미지 처리에 실패했습니다."),
	IMAGE_READ_FAILED(HttpStatus.BAD_REQUEST, "IMAGE008", "이미지 파일을 읽을 수 없습니다."),
	OPTION_IMAGE_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, "IMAGE009", "옵션 개수(%d)와 이미지 개수(%d)가 일치하지 않습니다."),
	IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE010", "이미지 업로드에 실패했습니다."),
	IMAGE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE011", "파일 크기가 제한을 초과했습니다."),
	// --- 태그 ---
	TAG_NOT_FOUND(HttpStatus.BAD_REQUEST, "TAG001", "존재하지 않는 태그입니다."),

	// --- 카테고리 ---
	CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "CATEGORY001", "존재하지 않는 카테고리입니다."),

	// --- 주문 ---
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER001", "존재하지 않는 회원입니다."),
	GROUPBUY_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER002", "존재하지 않는 공구 옵션입니다: %s"),
	GROUPBUY_OPTION_MISMATCH(HttpStatus.BAD_REQUEST, "ORDER003", "공구 ID와 옵션의 공구가 일치하지 않습니다: %s"),
	GROUPBUY_ENDED(HttpStatus.CONFLICT, "ORDER004", "종료된 공구입니다."),
	STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "ORDER005", "재고가 부족합니다. (요청: %d개, 사용가능: %d개)"),
	PERSONAL_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "ORDER006", "개인 구매 제한을 초과했습니다. 최대 %d개까지 구매 가능합니다."),
	ORDER_PROCESSING_IN_PROGRESS(HttpStatus.LOCKED, "ORDER007", "이미 진행 중인 주문이 있습니다. 잠시 후 다시 시도해주세요."),
	CART_ITEMS_EMPTY(HttpStatus.BAD_REQUEST, "ORDER008", "유효한 장바구니 아이템이 없습니다."),

	// --- 결제 ---
	PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT001", "존재하지 않는 결제입니다."),
	PAYMENT_NOT_PENDING(HttpStatus.CONFLICT, "PAYMENT002", "결제 대기 상태가 아닙니다."),
	PAYMENT_APPROVAL_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT003", "결제 승인에 실패했습니다."),
	PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PAYMENT004", "이미 결제가 진행 중입니다."),
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT005", "존재하지 않는 주문입니다."),
	ORDER_NOT_PENDING(HttpStatus.CONFLICT, "PAYMENT006", "주문이 결제 대기 상태가 아닙니다."),
	INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "PAYMENT007", "보유 포인트가 부족합니다."),
	TOSS_API_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT008", "토스 API 호출에 실패했습니다."),

	// --- 웹훅 관련 ---
	INVALID_SIGNATURE(HttpStatus.FORBIDDEN, "WEBHOOK001", "웹훅 서명이 유효하지 않습니다"),
	INVALID_JSON(HttpStatus.BAD_REQUEST, "WEBHOOK002", "웹훅 데이터 형식이 올바르지 않습니다"),
	WEBHOOK_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK003", "웹훅 처리 중 오류가 발생했습니다"),

	// --- 인증 ---
	INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH001", "유효하지 않은 토큰입니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 리프레시 토큰입니다."),
	MISSING_AUTHORIZATION_HEADER(HttpStatus.BAD_REQUEST, "AUTH003", "인증 헤더가 누락되었습니다."),
	EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH004", "만료된 토큰입니다."),
	MALFORMED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH005", "잘못된 형식의 토큰입니다."),
	UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH006", "지원하지 않는 소셜 제공자입니다: %s"),
	SOCIAL_TOKEN_EXCHANGE_FAILED(HttpStatus.UNAUTHORIZED, "AUTH007", "소셜 로그인 인증에 실패했습니다."),
	SOCIAL_MEMBER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "AUTH008", "회원 정보를 가져올 수 없습니다."),
	SOCIAL_LOGIN_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH009", "소셜 로그인 처리 중 오류가 발생했습니다."),
	REDIS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH010", "일시적인 서버 오류입니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH011", "접근 권한이 없습니다."),

	// --- 상품 ---
	PRODUCT_OPTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "PRODUCT001", "존재하지 않는 상품 옵션입니다"),
	PRODUCT_NOT_FOUND(HttpStatus.BAD_REQUEST, "PRODUCT002", "존재하지 않는 상품입니다."),
	CANNOT_DELETE_LAST_OPTION(HttpStatus.BAD_REQUEST, "PRODUCT003", "상품의 마지막 옵션은 삭제할 수 없습니다."),
	PRODUCT_OPTION_NOT_BELONG_TO_PRODUCT(HttpStatus.BAD_REQUEST, "PRODUCT004", "해당 옵션은 이 상품에 속하지 않습니다."),
	PRODUCT_NOT_EXIST(HttpStatus.BAD_REQUEST, "PRODUCT005", "공동구매 등록 가능한 상품 없습니다."),

	// --- 공동 구매 ---
	DISCOUNT_STAGES_PARSING_FAILED(HttpStatus.BAD_REQUEST, "GROUPBUY001", "할인 단계 정보를 파싱하는 데 실패했습니다."),
	INVALID_START_TIME(HttpStatus.BAD_REQUEST, "GROUPBUY002", "공동구매 시작일이 현재 시간보다 이전입니다."),
	INVALID_END_TIME(HttpStatus.BAD_REQUEST, "GROUPBUY003", "공동구매 종료일이 시작일보다 이전이거나 같습니다."),
	GROUP_BUY_DURATION_TOO_SHORT(HttpStatus.BAD_REQUEST, "GROUPBUY004", "공동구매 기간이 너무 짧습니다. 최소 1시간 이상이어야 합니다."),
	GROUP_BUY_DURATION_TOO_LONG(HttpStatus.BAD_REQUEST, "GROUPBUY005", "공동구매 기간이 너무 깁니다. 최대 7일까지 가능합니다."),
	ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUPBUY006", "요청된 엔티티를 찾을 수 없습니다."),
	PRODUCT_SELLER_MISMATCH(HttpStatus.FORBIDDEN, "GROUPBUY007", "상품의 판매자와 공동구매 등록자가 일치하지 않습니다."),
	PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "GROUPBUY008", "판매 불가능한 상품입니다."),
	OVERLAPPING_GROUP_BUY_EXISTS(HttpStatus.CONFLICT, "GROUPBUY009", "해당 상품의 공동구매가 이미 존재합니다."),
	INVALID_DISCOUNT_STAGES(HttpStatus.BAD_REQUEST, "GROUPBUY010", "할인 단계 정보가 올바르지 않습니다."),
	INVALID_DISCOUNT_STAGES_FOR_STOCK(HttpStatus.BAD_REQUEST, "GROUPBUY011", "재고량 대비 할인 단계 설정이 올바르지 않습니다."),
	GROUP_BUY_OPTION_OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "GROUPBUY012", "공동구매 옵션의 재고가 부족합니다."),
	DATABASE_CONSTRAINT_VIOLATION(HttpStatus.INTERNAL_SERVER_ERROR, "GROUPBUY013", "데이터베이스 제약조건을 위반했습니다."),
	GROUP_BUY_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "GROUPBUY014", "공동구매 시작일은 종료일보다 이전이어야 합니다."),
	DISCOUNT_RATE_REQUIRED(HttpStatus.BAD_REQUEST, "GROUPBUY015", "할인율은 필수입니다."),
	MIN_QUANTITY_REQUIRED(HttpStatus.BAD_REQUEST, "GROUPBUY016", "최소 달성 수량은 필수입니다."),
	DISCOUNT_RATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "GROUPBUY017", "할인율은 0~100% 사이여야 합니다."),
	MIN_QUANTITY_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "GROUPBUY018", "최소 달성 수량은 1 이상 10,000 이하여야 합니다."),
	DUPLICATE_DISCOUNT_STAGE(HttpStatus.BAD_REQUEST, "GROUPBUY019", "동일한 최소 수량의 할인 단계가 중복됩니다."),
	EXCEEDED_DISCOUNT_STAGE_LIMIT(HttpStatus.BAD_REQUEST, "GROUPBUY020", "할인 단계는 최대 10개까지 설정할 수 있습니다."),
	DISCOUNT_STAGE_EXCEEDS_STOCK(HttpStatus.BAD_REQUEST, "GROUPBUY021", "재고량보다 많은 최소 수량이 설정되어 있습니다."),
	GROUPBUY_NOT_FOUND(HttpStatus.BAD_REQUEST, "GROUPBUY022", "해당 공동구매를 찾을 수 없습니다."),
	GROUPBUY_DETAIL_IMAGES_TOO_MANY(HttpStatus.BAD_REQUEST, "GROUPBUY023", "상세 페이지 이미지 개수를 초과하였습니다."),
	GROUPBUY_SELLER_ACCESS_DENIED(HttpStatus.BAD_REQUEST,"GROUPBUY024", "다른 판매자의 공동구매에 접근할 수 없습니다."),
	INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "GROUPBUY025", "허용되지 않은 상태 변경입니다."),
	INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "GROUPBUY026", "현재 상태에서 요청한 상태로 변경할 수 없습니다."),
	GROUPBUY_NOT_STARTED_YET(HttpStatus.BAD_REQUEST, "GROUPBUY027", "공동구매 시작일이 아직 되지 않았습니다."),
	GROUPBUY_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "GROUPBUY028", "공동구매 종료일이 지났습니다."),
	GROUPBUY_NO_OPTIONS(HttpStatus.BAD_REQUEST, "GROUPBUY029", "공동구매에 옵션이 없습니다."),
	GROUPBUY_NO_STOCK(HttpStatus.BAD_REQUEST, "GROUPBUY030", "공동구매에 재고가 없습니다."),
	DISCOUNT_STAGE_QUANTITY_ORDER_INVALID(HttpStatus.BAD_REQUEST, "GROUPBUY031", "할인 단계의 최소 달성 수량이 순서대로 입력되지 않았습니다."),
	DISCOUNT_STAGE_RATE_ORDER_INVALID(HttpStatus.BAD_REQUEST, "GROUPBUY032", "할인 단계의 할인률이 순서대로 입력되지 않았습니다."),

	// --- 판매자 ---
	SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER001", "존재하지 않는 판매자입니다."),

	// --- 시스템 ---
	SYSTEM_TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SYSTEM001", "시스템 점검 중입니다. 1-2분 후 다시 시도해주세요.");


	private final HttpStatus status;
	private final String code;
	private final String message;

	ErrorCode(final HttpStatus status, final String code, final String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public String formatMessage(Object... args) {
		return String.format(this.message, args);
	}
}
