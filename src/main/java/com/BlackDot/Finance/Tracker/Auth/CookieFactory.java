package com.BlackDot.Finance.Tracker.Auth;

import com.BlackDot.Finance.Tracker.AppProps;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieFactory {
    public static final String REFRESH = "refresh_token";
    private final AppProps props;

    public ResponseCookie refresh(String raw) {
        return base(raw).maxAge(props.jwt().refreshTtl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(REFRESH, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite(props.cookieSecure() ? "None" : "Lax")
                .path("/api/v1/auth");
    }
}
