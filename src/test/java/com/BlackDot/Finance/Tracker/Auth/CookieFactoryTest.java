package com.BlackDot.Finance.Tracker.Auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.BlackDot.Finance.Tracker.AppProps;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CookieFactoryTest {
    @Test
    void usesCrossSiteCookieAttributesWhenSecure() {
        CookieFactory cookies = new CookieFactory(props(true));

        String cookie = cookies.refresh("token").toString();

        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));
    }

    @Test
    void keepsLocalDevelopmentCookieAttributesWhenInsecure() {
        CookieFactory cookies = new CookieFactory(props(false));

        String cookie = cookies.refresh("token").toString();

        assertTrue(!cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
    }

    private static AppProps props(boolean cookieSecure) {
        return new AppProps(
                "http://localhost:5173",
                cookieSecure,
                new AppProps.Jwt("secret", "issuer", Duration.ofMinutes(15), Duration.ofDays(30)));
    }
}
