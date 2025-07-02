package com.ururulab.ururu.groupBuy.event;

import com.ururulab.ururu.groupBuy.controller.dto.request.GroupBuyImageUploadRequest;

import java.util.List;

public record GroupBuyDetailImageUploadEvent(
        Long groupBuyId,
        List<GroupBuyImageUploadRequest> images
){}
