package com.BlackDot.Finance.Tracker.Auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.BlackDot.Finance.Tracker.AppProps;
import com.BlackDot.Finance.Tracker.Auth.MobileOAuth.MobileOAuthRequest;
import com.BlackDot.Finance.Tracker.Auth.MobileOAuth.MobileOAuthRequestFilter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthFailureHandler implements AuthenticationFailureHandler {
    private final AppProps props;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String code = exception instanceof OAuth2AuthenticationException oauthException
                ? oauthException.getError().getErrorCode()
                : "login_failed";

        HttpSession session = request.getSession(false);
        MobileOAuthRequest mobileRequest = null;
        if (session != null) {
            Object storedMobileRequest = session.getAttribute(MobileOAuthRequestFilter.SESSION_ATTRIBUTE);
            if (storedMobileRequest instanceof MobileOAuthRequest stored) {
                mobileRequest = stored;
                session.removeAttribute(MobileOAuthRequestFilter.SESSION_ATTRIBUTE);
            }
            session.invalidate();
        }
        if (mobileRequest != null) {
            response.sendRedirect(mobileRequest.redirectUri()
                    + "?error=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&state=" + URLEncoder.encode(mobileRequest.state(), StandardCharsets.UTF_8));
            return;
        }

        response.sendRedirect(props.frontendUrl() + "/login?error="
                + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }
}
