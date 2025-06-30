package com.ururulab.ururu.global.exception.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON001", "잘못된 HTTP 메서드를 호출했습니다."),

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

	// --- 상품 ---
	PRODUCT_OPTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "PRODUCT001", "존재하지 않는 상품 옵션입니다: %s"),
	PRODUCT_NOT_FOUND(HttpStatus.BAD_REQUEST, "PRODUCT002", "존재하지 않는 상품입니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH001", "접근 권한이 없습니다."),

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
