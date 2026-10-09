package com.BlackDot.Finance.Tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.jwt.secret=test-only-secret-that-is-at-least-32-bytes",
		"spring.security.oauth2.client.registration.google.client-id=test-google-client",
		"spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
		"spring.security.oauth2.client.registration.github.client-id=test-github-client",
		"spring.security.oauth2.client.registration.github.client-secret=test-github-secret"
})
class TrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
