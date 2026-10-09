package com.BlackDot.Finance.Tracker.Auth;

import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record OAuthProfile(String provider, String providerUserId, String email,
        boolean emailVerified, String name) {

    public static OAuthProfile from(String provider, OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        return switch (provider) {
            case "google" -> new OAuthProfile("google", (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    Boolean.TRUE.equals(attributes.get("email_verified")),
                    (String) attributes.get("name"));
            case "github" -> new OAuthProfile("github", String.valueOf(attributes.get("id")),
                    (String) attributes.get("email"),
                    Boolean.TRUE.equals(attributes.get("email_verified")),
                    attributes.get("name") != null ? (String) attributes.get("name")
                            : (String) attributes.get("login"));
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }
}
