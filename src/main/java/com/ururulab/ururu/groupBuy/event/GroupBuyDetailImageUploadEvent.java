package com.ururulab.ururu.groupBuy.event;

import com.ururulab.ururu.groupBuy.domain.dto.GroupBuyDetailImageRequest;

import java.util.List;

public record GroupBuyDetailImageUploadEvent(
        Long groupBuyId,
        List<GroupBuyDetailImageRequest> images
){}
