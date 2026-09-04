package com.ururulab.ururu.auth.oauth;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthPropertiesTest {

    @Test
    void kakaoRejectsMissingCredentials() {
        final KakaoOAuthProperties properties = new KakaoOAuthProperties();
        properties.setClientId("");
        properties.setClientSecret("");

        assertThatThrownBy(() -> properties.buildAuthorizationUrl("state"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
    }

    @Test
    void googleRejectsMissingCredentials() {
        final GoogleOAuthProperties properties = new GoogleOAuthProperties();
        properties.setClientId("");
        properties.setClientSecret("");

        assertThatThrownBy(() -> properties.buildAuthorizationUrl("state"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SOCIAL_LOGIN_NOT_CONFIGURED);
    }
}
