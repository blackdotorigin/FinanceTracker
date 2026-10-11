package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class MobileOAuthRequestFilterTest {
    private final MobileOAuthRequestFilter filter = new MobileOAuthRequestFilter();

    @Test
    void storesValidatedMobileParametersInTheOAuthSession() throws Exception {
        String state = "A".repeat(43);
        String challenge = "B".repeat(43);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");
        request.addParameter("app_redirect_uri", MobileOAuthRequestFilter.CALLBACK_URI);
        request.addParameter("app_state", state);
        request.addParameter("app_code_challenge", challenge);
        request.addParameter("app_code_challenge_method", "S256");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        MobileOAuthRequest stored = assertInstanceOf(
                MobileOAuthRequest.class,
                request.getSession().getAttribute(MobileOAuthRequestFilter.SESSION_ATTRIBUTE));
        assertEquals(MobileOAuthRequestFilter.CALLBACK_URI, stored.redirectUri());
        assertEquals(state, stored.state());
        assertEquals(challenge, stored.codeChallenge());
    }

    @Test
    void rejectsUnregisteredMobileRedirectBeforeOAuthStarts() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/oauth2/authorization/github");
        request.setServletPath("/oauth2/authorization/github");
        request.addParameter("app_redirect_uri", "https://attacker.example/callback");
        request.addParameter("app_state", "A".repeat(43));
        request.addParameter("app_code_challenge", "B".repeat(43));
        request.addParameter("app_code_challenge_method", "S256");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void rejectsMobileRequestWithoutS256Proof() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");
        request.addParameter("app_redirect_uri", MobileOAuthRequestFilter.CALLBACK_URI);
        request.addParameter("app_state", "A".repeat(43));
        request.addParameter("app_code_challenge", "B".repeat(43));
        request.addParameter("app_code_challenge_method", "plain");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(400, response.getStatus());
    }
}
