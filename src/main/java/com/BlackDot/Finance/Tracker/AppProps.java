package com.BlackDot.Finance.Tracker;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProps(String frontendUrl, boolean cookieSecure, Jwt jwt) {
    public record Jwt(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {}
}
