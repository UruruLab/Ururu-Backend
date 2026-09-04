package com.ururulab.ururu.auth.interceptor;

import com.ururulab.ururu.auth.annotation.RateLimit;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    @Test
    void limitsByRemoteAddressAndIgnoresUntrustedForwardedHeader() throws Exception {
        final StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), anyList(), anyString())).thenReturn(2L);
        final RateLimitInterceptor interceptor = new RateLimitInterceptor(redis, false);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertThatThrownBy(() -> interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new HandlerMethod(new LimitedHandler(), LimitedHandler.class.getMethod("login"))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOO_MANY_REQUESTS);

        verify(redis).execute(any(),
                org.mockito.ArgumentMatchers.eq(List.of("rate_limit:127.0.0.1:/login")), anyString());
    }

    static class LimitedHandler {
        @RateLimit(1)
        public void login() {
        }
    }
}
