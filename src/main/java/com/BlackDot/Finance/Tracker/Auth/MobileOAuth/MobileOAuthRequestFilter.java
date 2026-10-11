package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

import java.io.IOException;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

public final class MobileOAuthRequestFilter extends OncePerRequestFilter {
    public static final String SESSION_ATTRIBUTE = MobileOAuthRequest.class.getName();
    static final String CALLBACK_URI = "com.blackdotorigin.financeflow://auth/callback";
    private static final Pattern AUTHORIZATION_PATH =
            Pattern.compile("^/oauth2/authorization/(google|github)$");
    private static final Pattern URL_SAFE_TOKEN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!AUTHORIZATION_PATH.matcher(request.getServletPath()).matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        String redirectUri = request.getParameter("app_redirect_uri");
        String state = request.getParameter("app_state");
        String codeChallenge = request.getParameter("app_code_challenge");
        String codeChallengeMethod = request.getParameter("app_code_challenge_method");
        boolean hasMobileParameters = redirectUri != null
                || state != null
                || codeChallenge != null
                || codeChallengeMethod != null;
        HttpSession existingSession = request.getSession(false);

        if (!hasMobileParameters) {
            if (existingSession != null) {
                existingSession.removeAttribute(SESSION_ATTRIBUTE);
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!CALLBACK_URI.equals(redirectUri)
                || state == null
                || !URL_SAFE_TOKEN.matcher(state).matches()
                || codeChallenge == null
                || !URL_SAFE_TOKEN.matcher(codeChallenge).matches()
                || !"S256".equals(codeChallengeMethod)) {
            if (existingSession != null) {
                existingSession.removeAttribute(SESSION_ATTRIBUTE);
            }
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid mobile OAuth request");
            return;
        }

        request.getSession(true).setAttribute(
                SESSION_ATTRIBUTE,
                new MobileOAuthRequest(redirectUri, state, codeChallenge));
        filterChain.doFilter(request, response);
    }
}
