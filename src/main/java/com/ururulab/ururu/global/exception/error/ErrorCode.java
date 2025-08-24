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
	REQUEST_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE012", "전체 요청 크기가 제한을 초과했습니다."),
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
	ORDER_NOT_REFUNDABLE(HttpStatus.BAD_REQUEST, "ORDER009", "환불 가능한 주문 상태가 아닙니다."),
	ORDER_NOT_SHIPPABLE(HttpStatus.BAD_REQUEST, "ORDER010", "주문 상태가 배송 정보 등록이 불가능합니다."),
	TRACKING_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "ORDER011", "이미 운송장이 등록된 주문입니다."),

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

	// --- 환불 ---
	REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "REFUND001", "존재하지 않는 환불입니다."),
	REFUND_ALREADY_PROCESSED(HttpStatus.CONFLICT, "REFUND002", "이미 처리된 환불입니다."),
	REFUND_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "REFUND003", "환불 처리 기간이 만료되었습니다. (%d일 이내)"),
	DUPLICATE_REFUND_REQUEST(HttpStatus.CONFLICT, "REFUND004", "이미 진행 중인 환불 요청이 있습니다."),
	INVALID_REFUND_ACTION(HttpStatus.BAD_REQUEST, "REFUND005", "유효하지 않은 환불 처리 액션입니다."),

	// --- 인증 ---
	INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH001", "유효하지 않은 토큰입니다."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 리프레시 토큰입니다."),
	MISSING_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "AUTH003", "인증 헤더가 누락되었습니다."),
	MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH004", "리프레시 토큰이 누락되었습니다."),
	EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH005", "만료된 토큰입니다."),
	MALFORMED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH006", "잘못된 형식의 토큰입니다."),
	UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH007", "지원하지 않는 소셜 제공자입니다: %s"),
	SOCIAL_TOKEN_EXCHANGE_FAILED(HttpStatus.UNAUTHORIZED, "AUTH008", "소셜 로그인 인증에 실패했습니다."),
	SOCIAL_MEMBER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "AUTH009", "회원 정보를 가져올 수 없습니다."),
	SOCIAL_LOGIN_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH010", "소셜 로그인 처리 중 오류가 발생했습니다."),
	REDIS_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH011", "일시적인 서버 오류입니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH012", "접근 권한이 없습니다."),
	TOO_MANY_REFRESH_TOKENS(HttpStatus.TOO_MANY_REQUESTS, "AUTH013", "리프레시 토큰 개수가 제한을 초과했습니다. 다시 로그인해주세요."),
	INVALID_TOKEN_BLACKLIST_PARAMETERS(HttpStatus.BAD_REQUEST, "AUTH014", "토큰 블랙리스트 파라미터가 유효하지 않습니다."),
	TOKEN_BLACKLIST_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH015", "토큰 블랙리스트 작업에 실패했습니다."),
	INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "AUTH016", "유효하지 않은 OAuth state입니다."),
	TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH017", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
	CSRF_TOKEN_MISSING(HttpStatus.FORBIDDEN, "AUTH018", "CSRF 토큰이 누락되었습니다."),
	CSRF_TOKEN_EXPIRED(HttpStatus.FORBIDDEN, "AUTH019", "CSRF 토큰이 만료되었습니다."),
	CSRF_TOKEN_MISMATCH(HttpStatus.FORBIDDEN, "AUTH020", "CSRF 토큰이 일치하지 않습니다."),

	// --- 상품 ---
	PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT001", "존재하지 않는 상품 옵션입니다"),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT002", "존재하지 않는 상품입니다."),
	CANNOT_DELETE_LAST_OPTION(HttpStatus.BAD_REQUEST, "PRODUCT003", "상품의 마지막 옵션은 삭제할 수 없습니다."),
	PRODUCT_OPTION_NOT_BELONG_TO_PRODUCT(HttpStatus.BAD_REQUEST, "PRODUCT004", "해당 옵션은 이 상품에 속하지 않습니다."),
	PRODUCT_NOT_EXIST(HttpStatus.BAD_REQUEST, "PRODUCT005", "공동구매 등록 가능한 상품 없습니다."),

	// --- 공동 구매 ---
	DISCOUNT_STAGES_PARSING_FAILED(HttpStatus.BAD_REQUEST, "GROUPBUY001", "할인 단계 정보를 파싱하는 데 실패했습니다."),
	INVALID_START_TIME(HttpStatus.BAD_REQUEST, "GROUPBUY002", "공동구매 시작일이 현재 시간보다 이전입니다."),
	INVALID_END_TIME(HttpStatus.BAD_REQUEST, "GROUPBUY003", "공동구매 종료일이 시작일보다 이전이거나 같습니다."),
	GROUP_BUY_DURATION_TOO_SHORT(HttpStatus.BAD_REQUEST, "GROUPBUY004", "공동구매는 시작일과 종료일 사이에 최소 1시간 이상 간격이 있어야 합니다. 종료일을 다시 설정해 주세요."),
	GROUP_BUY_DURATION_TOO_LONG(HttpStatus.BAD_REQUEST, "GROUPBUY005", "공동구매는 최대 7일 동안만 진행할 수 있습니다. 종료일 기준으로 7일 이내에 시작일을 설정해 주세요."),
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
	MIN_QUANTITY_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "GROUPBUY018", "최소 달성 수량은 1 이상 9,999,999 이하여야 합니다."),
	DUPLICATE_DISCOUNT_STAGE(HttpStatus.BAD_REQUEST, "GROUPBUY019", "동일한 최소 수량의 할인 단계가 중복됩니다."),
	EXCEEDED_DISCOUNT_STAGE_LIMIT(HttpStatus.BAD_REQUEST, "GROUPBUY020", "할인 단계는 최대 10개까지 설정할 수 있습니다."),
	DISCOUNT_STAGE_EXCEEDS_STOCK(HttpStatus.BAD_REQUEST, "GROUPBUY021", "재고량보다 많은 최소 수량이 설정되어 있습니다."),
	GROUPBUY_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUPBUY022", "해당 공동구매를 찾을 수 없습니다."),
	GROUPBUY_DETAIL_IMAGES_TOO_MANY(HttpStatus.BAD_REQUEST, "GROUPBUY023", "상세 페이지 이미지 개수를 초과하였습니다."),
	GROUPBUY_SELLER_ACCESS_DENIED(HttpStatus.FORBIDDEN,"GROUPBUY024", "다른 판매자의 공동구매에 접근할 수 없습니다."),
	INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "GROUPBUY025", "허용되지 않은 상태 변경입니다."),
	INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "GROUPBUY026", "현재 상태에서 요청한 상태로 변경할 수 없습니다."),
	GROUPBUY_NOT_STARTED_YET(HttpStatus.BAD_REQUEST, "GROUPBUY027", "공동구매 시작일이 아직 되지 않았습니다."),
	GROUPBUY_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "GROUPBUY028", "공동구매 종료일이 지났습니다."),
	GROUPBUY_NO_OPTIONS(HttpStatus.BAD_REQUEST, "GROUPBUY029", "공동구매에 옵션이 없습니다."),
	GROUPBUY_NO_STOCK(HttpStatus.BAD_REQUEST, "GROUPBUY030", "공동구매에 재고가 없습니다."),
	DISCOUNT_STAGE_QUANTITY_ORDER_INVALID(HttpStatus.BAD_REQUEST, "GROUPBUY031", "할인 단계의 최소 달성 수량이 순서대로 입력되지 않았습니다."),
	DISCOUNT_STAGE_RATE_ORDER_INVALID(HttpStatus.BAD_REQUEST, "GROUPBUY032", "할인 단계의 할인률이 순서대로 입력되지 않았습니다."),
	GROUPBUY_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "GROUPBUY033", "DRAFT 상태인 공동구매만 삭제할 수 있습니다."),
	INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "GROUPBUY034", "유효하지 않은 검색어입니다."),
	GROUPBUY_EMPTY(HttpStatus.NOT_FOUND, "GROUPBUY035", "판매자의 공동구매가 존재하지 않습니다."),

	// -- 커서 --
	CURSOR_ENCODING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CURSOR001", "커서 인코딩에 실패했습니다"),
	CURSOR_DECODING_FAILED(HttpStatus.BAD_REQUEST, "CURSOR002", "커서 디코딩에 실패했습니다: %s"),

	// --- 공동구매 통계 ---
	GROUPBUY_STATISTICS_ALREADY_EXISTS(HttpStatus.CONFLICT, "GB_STAT_001", "이미 해당 공동구매의 통계가 존재합니다."),
	GROUPBUY_STATISTICS_NOT_FOUND(HttpStatus.NOT_FOUND, "GB_STAT_002", "공동구매 통계를 찾을 수 없습니다."),
	GROUPBUY_STATISTICS_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "GB_STAT_003", "공동구매 통계 계산에 실패했습니다."),


	// --- AI 서비스 ---

	// AI 서비스 연결 및 통신 에러
	AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 추천 서비스를 사용할 수 없습니다."),
	AI_SERVICE_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "AI002", "AI 서비스 응답 시간이 초과되었습니다."),
	AI_SERVICE_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "AI003", "AI 서비스 연결에 실패했습니다."),

	// AI 추천 처리 에러
	AI_RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI004", "상품 추천 생성에 실패했습니다."),
	AI_INVALID_REQUEST_FORMAT(HttpStatus.BAD_REQUEST, "AI005", "AI 서비스 요청 형식이 올바르지 않습니다."),
	AI_INVALID_RESPONSE_FORMAT(HttpStatus.BAD_GATEWAY, "AI006", "AI 서비스 응답 형식이 올바르지 않습니다."),
	AI_NO_RECOMMENDATIONS_FOUND(HttpStatus.NOT_FOUND, "AI007", "추천 가능한 상품이 없습니다."),

	// 회원 프로필 관련 에러 (기존 코드와 중복 방지를 위해 수정)
	BEAUTY_PROFILE_INCOMPLETE(HttpStatus.BAD_REQUEST, "AI008", "뷰티 프로필이 완성되지 않았습니다."),

	// AI 서비스 상태 관련 에러
	AI_SERVICE_HEALTH_CHECK_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI009", "AI 서비스 상태 확인에 실패했습니다."),
	AI_RECOMMENDATION_PROCESSING_IN_PROGRESS(HttpStatus.LOCKED, "AI010", "이미 추천 처리가 진행 중입니다. 잠시 후 다시 시도해주세요."),

	// --- 판매자 ---
	SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER001", "존재하지 않는 판매자입니다."),
	INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "SELLER002", "이메일 또는 비밀번호가 올바르지 않습니다."),
	INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN, "SELLER003", "비활성화된 계정입니다."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "SELLER004", "이미 사용 중인 이메일입니다."),
	DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "SELLER006", "이미 사용 중인 사업자등록번호입니다."),
	DUPLICATE_BRAND_NAME(HttpStatus.CONFLICT, "SELLER007", "이미 사용 중인 브랜드명입니다."),
	INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "SELLER005", "현재 비밀번호가 올바르지 않습니다."),
	
	// 판매자 검증 관련 에러
	SELLER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER008", "브랜드명은 필수입니다."),
	SELLER_BUSINESS_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER009", "사업자명은 필수입니다."),
	SELLER_OWNER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER010", "대표자명은 필수입니다."),
	SELLER_BUSINESS_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER011", "사업자등록번호는 필수입니다."),
	SELLER_BUSINESS_NUMBER_PATTERN_ERROR(HttpStatus.BAD_REQUEST, "SELLER012", "사업자등록번호 형식이 올바르지 않습니다."),
	SELLER_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER013", "이메일은 필수입니다."),
	SELLER_EMAIL_FORMAT_ERROR(HttpStatus.BAD_REQUEST, "SELLER014", "이메일 형식이 올바르지 않습니다."),
	SELLER_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER015", "비밀번호는 필수입니다."),
	SELLER_PASSWORD_SIZE_ERROR(HttpStatus.BAD_REQUEST, "SELLER016", "비밀번호는 8자 이상 20자 이하여야 합니다."),
	SELLER_PASSWORD_PATTERN_ERROR(HttpStatus.BAD_REQUEST, "SELLER017", "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."),
	SELLER_PHONE_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER018", "전화번호는 필수입니다."),
	SELLER_PHONE_FORMAT_ERROR(HttpStatus.BAD_REQUEST, "SELLER019", "전화번호 형식이 올바르지 않습니다."),
	SELLER_ADDRESS1_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER020", "기본주소는 필수입니다."),
	SELLER_ADDRESS2_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER021", "상세주소는 필수입니다."),
	SELLER_MAIL_ORDER_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER022", "통신판매업 신고번호는 필수입니다."),
	
	// 판매자 길이 검증 관련 에러
	SELLER_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER023", "브랜드명은 %d자 이하여야 합니다."),
	SELLER_BUSINESS_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER024", "사업자명은 %d자 이하여야 합니다."),
	SELLER_OWNER_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER025", "대표자명은 %d자 이하여야 합니다."),
	SELLER_BUSINESS_NUMBER_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER026", "사업자등록번호는 %d자여야 합니다."),
	SELLER_EMAIL_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER027", "이메일은 %d자 이하여야 합니다."),
	SELLER_PHONE_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER028", "전화번호는 %d자 이하여야 합니다."),
	SELLER_ZONECODE_REQUIRED(HttpStatus.BAD_REQUEST, "SELLER032", "우편번호는 필수입니다."),
	SELLER_ZONECODE_SIZE_ERROR(HttpStatus.BAD_REQUEST, "SELLER033", "우편번호는 %d자리여야 합니다."),
	SELLER_ZONECODE_PATTERN_ERROR(HttpStatus.BAD_REQUEST, "SELLER034", "우편번호는 숫자만 입력 가능합니다."),
	SELLER_ADDRESS1_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER029", "기본주소는 %d자 이하여야 합니다."),
	SELLER_ADDRESS2_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER030", "상세주소는 %d자 이하여야 합니다."),
	SELLER_MAIL_ORDER_NUMBER_TOO_LONG(HttpStatus.BAD_REQUEST, "SELLER031", "통신판매업 신고번호는 %d자 이하여야 합니다."),

	// --- 시스템 ---
	SYSTEM_TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SYSTEM001", "시스템 점검 중입니다. 1-2분 후 다시 시도해주세요."),

	// --- 회원 ---
	MEMBER_NOT_EXIST(HttpStatus.NOT_FOUND, "MEMBER001", "회원을 찾을 수 없습니다."),
	MEMBER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "MEMBER002", "이미 탈퇴한 회원입니다."),
	MEMBER_DELETION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MEMBER003", "회원 탈퇴가 불가능한 상태입니다."),
	DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "MEMBER004", "이미 사용 중인 닉네임입니다."),
	DUPLICATE_EMAIL_MEMBER(HttpStatus.CONFLICT, "MEMBER005", "이미 사용 중인 이메일입니다."),
	INVALID_MEMBER_STATUS(HttpStatus.BAD_REQUEST, "MEMBER006", "유효하지 않은 회원 상태입니다."),
	MEMBER_ACTIVE_ORDERS_EXIST(HttpStatus.BAD_REQUEST, "MEMBER007", "진행 중인 주문이 %d건 있어 탈퇴할 수 없습니다."),
	MEMBER_PENDING_PAYMENTS_EXIST(HttpStatus.BAD_REQUEST, "MEMBER008", "진행 중인 결제가 있어 탈퇴할 수 없습니다."),
	PROFILE_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "MEMBER009", "이미지 파일은 필수입니다."),
	INVALID_GENDER_VALUE(HttpStatus.BAD_REQUEST, "MEMBER010", "올바른 성별 값이 아닙니다."),
	MEMBER_DELETION_FAILED(HttpStatus.BAD_REQUEST, "MEMBER011", "회원 데이터 정리 중 오류가 발생했습니다"),
	MEMBER_DELETED(HttpStatus.BAD_REQUEST, "MEMBER012", "탈퇴한 회원입니다."),
	MEMBER_LOGIN_DENIED(HttpStatus.BAD_REQUEST,"MEMBER013", "탈퇴한 회원은 로그인할 수 없습니다."),

	// --- 뷰티 프로필 ---
	BEAUTY_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "BEAUTY001", "뷰티 프로필을 찾을 수 없습니다."),
	BEAUTY_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "BEAUTY002", "이미 뷰티 프로필이 존재합니다."),
	INVALID_SKIN_TYPE(HttpStatus.BAD_REQUEST, "BEAUTY003", "올바른 피부 타입 값이 아닙니다."),
	INVALID_SKIN_TONE(HttpStatus.BAD_REQUEST, "BEAUTY004", "올바른 피부 톤 값이 아닙니다."),
	INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "BEAUTY005", "최소 가격은 최대 가격보다 작거나 같아야 합니다."),
	ALLERGY_INCONSISTENCY(HttpStatus.BAD_REQUEST, "BEAUTY006", "알러지가 있다고 선택하셨습니다. 알러지 목록을 입력해주세요."),
	NO_ALLERGY_INCONSISTENCY(HttpStatus.BAD_REQUEST, "BEAUTY007", "알러지가 없다고 선택하셨습니다. 알러지 목록을 비워주세요."),

	// --- 회원 약관 동의 ---
	INVALID_AGREEMENT_REQUEST(HttpStatus.BAD_REQUEST, "AGREEMENT001", "약관 동의 요청이 올바르지 않습니다."),
	AGREEMENT_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "AGREEMENT002", "존재하지 않는 약관 타입입니다."),
	REQUIRED_AGREEMENT_NOT_AGREED(HttpStatus.BAD_REQUEST, "AGREEMENT003", "필수 약관에 동의해야 합니다."),

	// --- 회원 선호도 ---
	MEMBER_PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PREFERENCE001", "회원 선호도를 찾을 수 없습니다."),
	MEMBER_PREFERENCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PREFERENCE002", "해당 판매자에 대한 선호도가 이미 존재합니다."),
	INVALID_PREFERENCE_LEVEL(HttpStatus.BAD_REQUEST, "PREFERENCE003", "선호도 레벨은 1-5 사이여야 합니다."),
	INVALID_PURCHASE_FREQUENCY(HttpStatus.BAD_REQUEST, "PREFERENCE004", "올바른 구매 빈도 값이 아닙니다."),
	PURCHASE_FREQUENCY_REQUIRED(HttpStatus.BAD_REQUEST, "PREFERENCE005", "구매 빈도는 필수입니다."),

	// --- 배송지 ---
	SHIPPING_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIPPING001", "배송지를 찾을 수 없습니다."),
	SHIPPING_ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "SHIPPING002", "배송지는 최대 5개까지 등록할 수 있습니다."),
	DEFAULT_SHIPPING_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "SHIPPING003", "기본 배송지를 찾을 수 없습니다."),
	SHIPPING_ADDRESS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SHIPPING004", "해당 배송지에 접근할 권한이 없습니다.");

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
