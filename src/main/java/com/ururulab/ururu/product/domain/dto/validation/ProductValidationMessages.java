package com.ururulab.ururu.product.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductValidationMessages {

    // Product 관련 메시지
    public static final String PRODUCT_NAME_REQUIRED = "상품명은 필수입니다";
    public static final String PRODUCT_NAME_SIZE = "상품명은 " + ProductValidationConstants.PRODUCT_NAME_MAX + "자를 초과할 수 없습니다";
    public static final String PRODUCT_DESCRIPTION_REQUIRED = "상품 설명은 필수입니다";
    public static final String CATEGORIES_REQUIRED = "카테고리는 최소 1개 이상 선택해야 합니다";
    public static final String PRODUCT_OPTIONS_REQUIRED = "상품 옵션은 최소 1개 이상 등록해야 합니다";
    public static final String PRODUCT_NOTICE_REQUIRED = "상품 정보고시는 필수입니다";

    // ProductOption 관련 메시지
    public static final String OPTION_NAME_REQUIRED = "옵션명은 필수입니다";
    public static final String OPTION_PRICE_REQUIRED = "가격은 필수입니다";
    public static final String FULL_INGREDIENTS_REQUIRED = "전성분은 필수입니다";

    // ProductNotice 관련 메시지
    public static final String CAPACITY_REQUIRED = "용량은 필수입니다";
    public static final String CAPACITY_SIZE = "용량은 " + ProductValidationConstants.CAPACITY_MAX + "자를 초과할 수 없습니다";
    public static final String SPEC_REQUIRED = "제품 주요 사양은 필수입니다";
    public static final String SPEC_SIZE = "제품 주요 사양은 " + ProductValidationConstants.SPEC_MAX + "자를 초과할 수 없습니다";
    public static final String EXPIRY_REQUIRED = "사용기한은 필수입니다";
    public static final String EXPIRY_SIZE = "사용기한은 " + ProductValidationConstants.EXPIRY_MAX + "자를 초과할 수 없습니다";
    public static final String USAGE_REQUIRED = "사용방법은 필수입니다";
    public static final String MANUFACTURER_REQUIRED = "제조업자는 필수입니다";
    public static final String MANUFACTURER_SIZE = "제조업자는 " + ProductValidationConstants.MANUFACTURER_MAX + "자를 초과할 수 없습니다";
    public static final String RESPONSIBLE_SELLER_REQUIRED = "책임판매업자는 필수입니다";
    public static final String RESPONSIBLE_SELLER_SIZE = "책임판매업자는 " + ProductValidationConstants.RESPONSIBLE_SELLER_MAX + "자를 초과할 수 없습니다";
    public static final String COUNTRY_OF_ORIGIN_REQUIRED = "제조국은 필수입니다";
    public static final String COUNTRY_OF_ORIGIN_SIZE = "제조국은 " + ProductValidationConstants.COUNTRY_OF_ORIGIN_MAX + "자를 초과할 수 없습니다";
    public static final String FUNCTIONAL_COSMETICS_REQUIRED = "기능성 화장품 여부는 필수입니다";
    public static final String CAUTION_REQUIRED = "주의사항은 필수입니다";
    public static final String WARRANTY_REQUIRED = "품질보증기준은 필수입니다";
    public static final String CUSTOMER_SERVICE_NUMBER_REQUIRED = "고객센터 번호는 필수입니다";
    public static final String CUSTOMER_SERVICE_NUMBER_SIZE = "고객센터 번호는 " + ProductValidationConstants.CUSTOMER_SERVICE_NUMBER_MAX + "자를 초과할 수 없습니다";
}
