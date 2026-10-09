package com.BlackDot.Finance.Tracker.Auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GithubAwareUserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestClient rest = RestClient.create();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(request);
        if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
            return user;
        }

        try {
            List<Map<String, Object>> emails = rest.get()
                    .uri("https://api.github.com/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.getAccessToken().getTokenValue())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            String email = emails == null ? null : emails.stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.get("primary"))
                            && Boolean.TRUE.equals(entry.get("verified")))
                    .map(entry -> (String) entry.get("email"))
                    .findFirst()
                    .orElse(null);

            Map<String, Object> attributes = new HashMap<>(user.getAttributes());
            attributes.put("email", email);
            attributes.put("email_verified", email != null);
            return new DefaultOAuth2User(user.getAuthorities(), attributes, "id");
        } catch (RestClientException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("github_email_failed"), exception);
        }
    }
}
