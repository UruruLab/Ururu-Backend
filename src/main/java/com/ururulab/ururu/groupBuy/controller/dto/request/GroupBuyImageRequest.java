package com.ururulab.ururu.groupBuy.controller.dto.request;

import jakarta.validation.constraints.NotNull;

public record GroupBuyImageRequest (
        Long id, // Update시에만 사용 (Create시에는 null)

        @NotNull(message = "이미지 순서는 필수입니다")
        Integer displayOrder,

        String detailImageUrl // 수정 시에만 사용, 새로 생성시에는 MultipartFile로 별도 처리
){
}
