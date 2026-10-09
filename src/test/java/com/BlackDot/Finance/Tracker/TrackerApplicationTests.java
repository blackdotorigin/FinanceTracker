package com.BlackDot.Finance.Tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-only-secret-that-is-at-least-32-bytes")
class TrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
