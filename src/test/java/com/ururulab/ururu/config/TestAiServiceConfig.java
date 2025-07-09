package com.ururulab.ururu.config;

import com.ururulab.ururu.global.client.AiServiceClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 AI 서비스 설정.
 *
 * <p>실제 AI 서비스에 의존하지 않고 테스트가 실행될 수 있도록 MockBean을 제공합니다.</p>
 */
@TestConfiguration
@Profile("test")
public class TestAiServiceConfig {

    @MockBean
    private AiServiceClient aiServiceClient;
}
