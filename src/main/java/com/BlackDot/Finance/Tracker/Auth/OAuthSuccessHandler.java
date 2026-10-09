package com.BlackDot.Finance.Tracker.Auth;

import java.io.IOException;
import com.BlackDot.Finance.Tracker.AppProps;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuthUserService oauthUsers;
    private final TokenService tokens;
    private final CookieFactory cookies;
    private final AppProps props;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        try {
            OAuthProfile profile = OAuthProfile.from(
                    token.getAuthorizedClientRegistrationId(), token.getPrincipal());
            AuthTokens issued = tokens.issue(oauthUsers.findOrCreate(profile));
            response.addHeader(HttpHeaders.SET_COOKIE, cookies.refresh(issued.refreshToken()).toString());

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect( "http://localhost:8080/" + "api/v1/users/me");
        } catch (OAuth2AuthenticationException exception) {
            response.sendRedirect(props.frontendUrl() + "/login?error="
                    + exception.getError().getErrorCode());
        }
    }
}
