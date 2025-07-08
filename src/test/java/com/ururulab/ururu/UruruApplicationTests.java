package com.ururulab.ururu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"toss.payments.secret-key=test_dummy_key",
		"toss.payments.client-key=test_dummy_ckey",
		"toss.payments.base-url=https://dummy.api.toss.com"
})
class UruruApplicationTests {

	@Test
	void contextLoads() {
	}

}