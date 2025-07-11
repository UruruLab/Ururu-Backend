package com.ururulab.ururu.groupBuy.dto.request;

import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.seller.domain.entity.Seller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationConstants.*;
import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationMessages.*;

public record GroupBuyRequest(
        @NotBlank(message = GROUP_BUY_TITLE_REQUIRED)
        @Size(max = GROUP_BUY_TITLE_MAX, message = GROUP_BUY_TITLE_SIZE)
        String title,

        @Size(max = GROUP_BUY_DESCRIPTION_MAX, message = GROUP_BUY_DESCRIPTION_SIZE)
        String description,

        @NotNull(message = PRODUCT_ID_REQUIRED)
        Long productId,

        @NotEmpty(message = DISCOUNT_STAGES_REQUIRED)
        @Valid
        List<DiscountStageDto> discountStages, // JSON 형태의 할인 단계 정보

        @NotNull(message = LIMIT_QUANTITY_REQUIRED)
        @Min(value = LIMIT_QUANTITY_MIN, message = LIMIT_QUANTITY_MIN_MSG)
        @Max(value = LIMIT_QUANTITY_MAX, message = LIMIT_QUANTITY_MAX_MSG)
        Integer limitQuantityPerMember,

        //@NotNull(message = START_AT_REQUIRED)
        //Instant startAt,

        @NotNull(message = ENDS_AT_REQUIRED)
        Instant endsAt,

        @Valid
        @NotEmpty(message = GROUP_BUY_OPTIONS_REQUIRED)
        List<GroupBuyOptionRequest> options,

        @Valid
        List<GroupBuyImageRequest> images
) {
    public GroupBuy toEntity(Product product, Seller seller, String thumbnailUrl) {
        String discountStagesJson = DiscountStageParser.toJsonString(discountStages);
        return GroupBuy.of(
                product,
                seller,
                title,
                description,
                thumbnailUrl,
                discountStagesJson,
                limitQuantityPerMember,
                GroupBuyStatus.DRAFT, // 기본값 - 작성 중 상태
                //startAt,
                null, //startAt
                endsAt
        );
    }
}
