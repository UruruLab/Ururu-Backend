package com.ururulab.ururu.groupBuy.controller.dto.validation;

import lombok.experimental.UtilityClass;

import static com.ururulab.ururu.groupBuy.controller.dto.validation.GroupBuyValidationConstants.*;

@UtilityClass
public class GroupBuyValidationMessages {

    public static final String GROUP_BUY_TITLE_REQUIRED = "공동구매 제목은 필수입니다";
    public static final String GROUP_BUY_TITLE_SIZE = "공동구매 제목은 " + GROUP_BUY_TITLE_MAX + "자를 초과할 수 없습니다";
    public static final String GROUP_BUY_DESCRIPTION_SIZE = "공동구매 설명은 " + GROUP_BUY_DESCRIPTION_MAX + "자를 초과할 수 없습니다";

    public static final String PRODUCT_ID_REQUIRED = "상품 ID는 필수입니다";
    public static final String DISCOUNT_STAGES_REQUIRED = "할인 단계 정보는 필수입니다";

    public static final String LIMIT_QUANTITY_REQUIRED = "1인 최대 수량은 필수입니다";
    public static final String LIMIT_QUANTITY_MIN_MSG = "1인 최대 수량은 " + LIMIT_QUANTITY_MIN + "개 이상이어야 합니다";
    public static final String LIMIT_QUANTITY_MAX_MSG = "1인 최대 수량은 " + LIMIT_QUANTITY_MAX + "개를 초과할 수 없습니다";

    public static final String START_AT_REQUIRED = "공동구매 시작일은 필수입니다";
    public static final String ENDS_AT_REQUIRED = "공동구매 종료일은 필수입니다";

    // 옵션 관련 메시지
    public static final String GROUP_BUY_OPTIONS_REQUIRED = "공동구매 옵션은 최소 1개 이상 등록해야 합니다";
    public static final String PRODUCT_OPTION_ID_REQUIRED = "상품 옵션 ID는 필수입니다";
    public static final String STOCK_REQUIRED = "재고는 필수입니다";
    public static final String STOCK_MIN = "재고는 0 이상이어야 합니다";
    public static final String PRICE_OVERRIDE_REQUIRED = "공구 시작가는 필수입니다";
    public static final String PRICE_OVERRIDE_MIN = "공구 시작가는 0 이상이어야 합니다";
    public static final String SALE_PRICE_REQUIRED = "실제 판매가는 필수입니다";
    public static final String SALE_PRICE_MIN = "실제 판매가는 0 이상이어야 합니다";

    // 이미지 관련 메시지
    public static final String GROUP_BUY_THUMBNAIL_IMAGES_TOO_MANY = "공동구매 썸네일 이미지는 최대 " + MAX_GROUP_BUY_THUMBNAIL_IMAGES + "개까지 등록 가능합니다";
    public static final String GROUP_BUY_DETAIL_IMAGES_TOO_MANY = "공동구매 썸네일 이미지는 최대 " + MAX_GROUP_BUY_DETAIL_IMAGES + "개까지 등록 가능합니다";
}
