package com.ururulab.ururu.groupBuy.dto.request;

import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import jakarta.validation.constraints.NotNull;

public record GroupBuyStatusUpdateRequest(
        @NotNull(message = "상태는 필수입니다")
        GroupBuyStatus status
) {
}
