package com.BlackDot.Finance.Tracker.Auth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.BlackDot.Finance.Tracker.AppProps;
import com.BlackDot.Finance.Tracker.Auth.MobileOAuth.MobileOAuthHandoffService;
import com.BlackDot.Finance.Tracker.Auth.MobileOAuth.MobileOAuthRequest;
import com.BlackDot.Finance.Tracker.Auth.MobileOAuth.MobileOAuthRequestFilter;
import com.BlackDot.Finance.Tracker.Users.User;
import com.BlackDot.Finance.Tracker.UserActivity.UserActivityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuthUserService oauthUsers;
    private final TokenService tokens;
    private final MobileOAuthHandoffService mobileOAuthHandoffs;
    private final CookieFactory cookies;
    private final AppProps props;
    private final UserActivityService userActivity;

    public OAuthSuccessHandler(
            OAuthUserService oauthUsers,
            TokenService tokens,
            MobileOAuthHandoffService mobileOAuthHandoffs,
            CookieFactory cookies,
            AppProps props,
            UserActivityService userActivity) {
        this.oauthUsers = oauthUsers;
        this.tokens = tokens;
        this.mobileOAuthHandoffs = mobileOAuthHandoffs;
        this.cookies = cookies;
        this.props = props;
        this.userActivity = userActivity;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        try {
            HttpSession session = request.getSession(false);
            MobileOAuthRequest mobileRequest = null;
            if (session != null) {
                Object storedMobileRequest = session.getAttribute(MobileOAuthRequestFilter.SESSION_ATTRIBUTE);
                if (storedMobileRequest instanceof MobileOAuthRequest stored) {
                    mobileRequest = stored;
                    session.removeAttribute(MobileOAuthRequestFilter.SESSION_ATTRIBUTE);
                }
            }

            OAuthProfile profile = OAuthProfile.from(
                    token.getAuthorizedClientRegistrationId(), token.getPrincipal());
            User user = oauthUsers.findOrCreate(profile);

            if (session != null) {
                session.invalidate();
            }

            if (mobileRequest != null) {
                String handoffCode = mobileOAuthHandoffs.issue(
                        user, mobileRequest.state(), mobileRequest.codeChallenge());
                response.sendRedirect(mobileRequest.redirectUri()
                        + "?code=" + URLEncoder.encode(handoffCode, StandardCharsets.UTF_8)
                        + "&state=" + URLEncoder.encode(mobileRequest.state(), StandardCharsets.UTF_8));
                return;
            }

            AuthTokens issued = tokens.issue(user);
            userActivity.recordLogin(user.getId());
            response.addHeader(HttpHeaders.SET_COOKIE, cookies.refresh(issued.refreshToken()).toString());
            response.sendRedirect(props.frontendUrl() + "/auth/callback");
        } catch (OAuth2AuthenticationException exception) {
            response.sendRedirect(props.frontendUrl() + "/login?error="
                    + URLEncoder.encode(exception.getError().getErrorCode(), StandardCharsets.UTF_8));
        }
    }
}
