package com.ururulab.ururu;

import com.ururulab.ururu.config.TestAiServiceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAiServiceConfig.class)
@TestPropertySource(properties = {
		"toss.payments.secret-key=test_dummy_key",
		"toss.payments.client-key=test_dummy_ckey",
		"toss.payments.base-url=https://dummy.api.toss.com"
})
class UruruApplicationTests {

	@Test
	void contextLoads() {
		// Spring Context가 정상적으로 로드되는지 확인
	}

}
