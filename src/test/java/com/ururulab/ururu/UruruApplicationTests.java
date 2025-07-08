package com.ururulab.ururu;

import com.ururulab.ururu.config.TestAiServiceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAiServiceConfig.class)
class UruruApplicationTests {

	@Test
	void contextLoads() {
		// Spring Context가 정상적으로 로드되는지 확인
	}

}
