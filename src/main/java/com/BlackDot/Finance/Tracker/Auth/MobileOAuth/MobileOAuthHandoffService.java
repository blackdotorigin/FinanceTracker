package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import com.BlackDot.Finance.Tracker.Auth.AuthTokens;
import com.BlackDot.Finance.Tracker.Auth.TokenService;
import com.BlackDot.Finance.Tracker.Auth.UnauthorizedException;
import com.BlackDot.Finance.Tracker.UserActivity.UserActivityService;
import com.BlackDot.Finance.Tracker.Users.User;
import com.BlackDot.Finance.Tracker.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MobileOAuthHandoffService {
    private static final Duration HANDOFF_TTL = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final MobileOAuthHandoffRepository handoffs;
    private final UserRepository users;
    private final TokenService tokens;
    private final UserActivityService userActivity;

    @Transactional
    public String issue(User user, String state, String codeChallenge) {
        if (!isUrlSafeToken(state) || !isUrlSafeToken(codeChallenge)) {
            throw new IllegalArgumentException("Invalid mobile OAuth handoff details");
        }

        Instant now = Instant.now();
        handoffs.deleteByExpiresAtBefore(now);

        byte[] randomCode = new byte[32];
        RANDOM.nextBytes(randomCode);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomCode);

        MobileOAuthHandoff handoff = new MobileOAuthHandoff();
        handoff.setUserId(user.getId());
        handoff.setCodeHash(hash(code));
        handoff.setCodeChallenge(codeChallenge);
        handoff.setState(state);
        handoff.setExpiresAt(now.plus(HANDOFF_TTL));
        handoffs.save(handoff);
        return code;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthTokens exchange(String code, String codeVerifier, String state) {
        if (code == null || !isUrlSafeToken(code) || codeVerifier == null
                || !isCodeVerifier(codeVerifier) || !isUrlSafeToken(state)) {
            throw new UnauthorizedException("Invalid mobile OAuth code");
        }

        Instant now = Instant.now();
        String codeHash = hash(code);
        String codeChallenge = createCodeChallenge(codeVerifier);
        if (handoffs.consume(codeHash, codeChallenge, state, now) != 1) {
            throw new UnauthorizedException("Invalid or expired mobile OAuth code");
        }

        MobileOAuthHandoff handoff = handoffs.findByCodeHash(codeHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid mobile OAuth code"));
        User user = users.findByIdAndActiveTrue(handoff.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Account disabled"));
        AuthTokens issued = tokens.issue(user);
        userActivity.recordLogin(user.getId());
        return issued;
    }

    static String createCodeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean isUrlSafeToken(String value) {
        return value != null && value.matches("^[A-Za-z0-9_-]{43}$");
    }

    private static boolean isCodeVerifier(String value) {
        return value.length() >= 43
                && value.length() <= 128
                && value.matches("^[A-Za-z0-9._~-]+$");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
