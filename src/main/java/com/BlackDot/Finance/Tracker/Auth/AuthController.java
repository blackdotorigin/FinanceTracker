package com.BlackDot.Finance.Tracker.Auth;

import com.BlackDot.Finance.Tracker.Users.User;
import com.BlackDot.Finance.Tracker.Users.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final TokenService tokens;
    private final CookieFactory cookies;
    private final UserRepository users;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username().trim().toLowerCase(), request.password()));
        AuthUser principal = (AuthUser) auth.getPrincipal();
        User user = users.findByIdAndActiveTrue(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        return respond(tokens.issue(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = CookieFactory.REFRESH, required = false) String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("Missing refresh token");
        }
        return respond(tokens.rotate(raw));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieFactory.REFRESH, required = false) String raw) {
        if (raw != null && !raw.isBlank()) {
            tokens.revoke(raw);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    private ResponseEntity<TokenResponse> respond(AuthTokens authTokens) {
        ResponseCookie refreshCookie = cookies.refresh(authTokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new TokenResponse(authTokens.accessToken(), "Bearer", authTokens.expiresInSeconds()));
    }
}
