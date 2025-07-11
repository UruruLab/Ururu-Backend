package com.ururulab.ururu.groupBuy.util;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class AuthUtils {

    /**
     * JWT 토큰에서 판매자 ID를 추출하는 헬퍼 메서드
     */
    public Long getSellerIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long)) {
            throw new BusinessException(ErrorCode.MALFORMED_JWT_TOKEN);
        }

        // 판매자 권한 확인
        boolean isSeller = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SELLER".equals(authority.getAuthority()));

        if (!isSeller) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return (Long) principal;
    }
}
